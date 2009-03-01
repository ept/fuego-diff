#!/usr/bin/python

"""
A little script for determining dependencies between classes and/or packages.
Statically analyses Java source. Not perfect, but useful a first
approximation to help find obsolete code.

Run this script in the source directory, writing its output to a file:

    python deps.py > deps.dot

If you don't have Graphviz, install it from http://www.graphviz.org/
then run something like:

    fdp -Tps -O -Gviewport=400,100,0.06 deps.dot

(If you don't have a binary called `fdp`, try creating a symlink to `dot`.)
This should create a PostScript file called deps.dot.ps with the graph in it.
"""

import os
import re

CLASS_LEVEL = False # False: nodes are packages; True: nodes are classes

found = []

def process(filename):
    qname = re.sub(r'^\.*(.*)\.java', r'\1', re.sub('/', '.', filename))
    packname = re.sub(r'\.([^\.]*)$', '', qname)
    imports = {}
    file = open(filename, 'r')
    while True:
        line = file.readline()
        if not line: break
        matches = re.match(r'import ([a-z0-9\.]+)\.([A-Z][A-Za-z0-9]*);', line)
        if matches:
            imports[matches.group(2)] = "%s.%s" % (matches.group(1), matches.group(2))
        unqual = '|'.join([shortname for (shortname, qualname) in imports.items()])
        qual = r'[a-z0-9\.]+\.[A-Z][A-Za-z0-9]*'
        find_idents = qual if unqual == '' else '%s|%s' % (qual, unqual)
        find_idents = r'(^|[^\w\.])(%s)' % find_idents
        idents = [ident for (dummy, ident) in re.findall(find_idents, line)]
        for ident in idents:
            ident = imports[ident] if ident in imports else ident
            if ident.startswith('java'): continue
            pair = (qname, ident) if CLASS_LEVEL else (packname, re.sub(r'\.([^\.]*)$', '', ident))
            if pair not in found:
                found.append(pair)
                print '  "%s" -> "%s";' % pair
    file.close()

def directory(dummy, dirname, files):
    for file in files:
        if file.endswith('.java'):
            process(os.path.join(dirname, file))

print "digraph G {"
os.path.walk('.', directory, None)
print "}"
