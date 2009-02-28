Fuego Diff -- A structural XML diff and patch library
=====================================================

Fuego Diff is a tool and library for calculating the differences between two
XML documents (a bit like the `diff` tool does for text files), and for
applying edits to a document (like the `patch` tool). It can thus be used e.g.
for three-way merging between documents modified by different people.

Feature highlights:
* Structural diff based on XML semantics (attribute order does not matter,
  child element order does matter)
* Detects movement of subtrees from one part of the document to another
* Supports Unicode and XML Namespaces
* Implemented in pure Java

This library was originally developed at the Helsinki Open Source Laboratory
(HOSLAB) at the University of Helsinki, under the name
[Fuego Core XML Diff and Patch Tool](http://hoslab.cs.helsinki.fi/homepages/fc-xmldiff/).
The project was forked by [Martin Kleppmann](http://www.yes-no-cancel.co.uk)
in February 2009, to continue development under the new name
[Fuego Diff](http://github.com/ept/fuego-diff/). The underlying algorithms have
been described in T. Lindholm, J. Kangasharju, and S. Tarkoma: 
[Fast and Simple XML Tree Differencing by Sequence Alignment](http://www.hiit.fi/files/fi/fc/papers/doceng06-pc.pdf), 
Proceedings of ACM DocEng 2006
([official version of the paper at acm.org](http://doi.acm.org/10.1145/1166160.1166183)).


Installation
------------

Fuego Diff is currently available only in source form. You can download the
source from GitHub:
* Download Fuego Diff [as tarball](http://github.com/ept/fuego-diff/tarball/master)
* Download Fuego Diff [as ZIP file](http://github.com/ept/fuego-diff/zipball/master)

...or you can clone the [git](http://git-scm.com/) repository by typing:

    git clone git://github.com/ept/fuego-diff.git

Next, go into the directory and download the project's dependencies by typing:

    ant -f project-setup.xml

Then you can build the project simply by typing:

    ant

You can see it in operation by giving it two XML files, `file1.xml` and
`file2.xml` for example, and writing the diff to a file called `diff.xml`:

    ant -Dbase=file1.xml -Dnew=file2.xml -Ddiff=diff.xml diff

And to apply the patch `diff.xml` to `file1.xml`, writing the result to
`new.xml`, try this:

    ant -Dbase=file1.xml -Ddiff=diff.xml -Dnew=new.xml patch


Development status
------------------

Fuego Diff is currently being refactored significantly, and the patch format
or behaviour may change at any time. Feel free to use it and send me comments,
bug reports, patches or pull requests. However I wouldn't recommend putting it
to production use just yet. Hopefully soon.

You can contact me by email: martin at eptcomputing dot com.


Copyright
---------

Copyright 2005--2008 Helsinki Institute for Information Technology.

Copyright 2009 Martin Kleppmann.

Fuego Diff is free software; you can redistribute it and/or modify it under the
terms of the MIT license. See the file `MIT-LICENSE` in the source distribution
for details.
