# Copyright 2006 Helsinki Institute for Information Technology

# This file is a part of Fuego middleware.  Fuego middleware is free
# software; you can redistribute it and/or modify it under the terms
# of the MIT license, included as the file MIT-LICENSE in the Fuego
# middleware source distribution.  If you did not receive the MIT
# license with the distribution, write to the Fuego Core project at
# fuego-core-users@hoslab.cs.helsinki.fi.

# Makefile for Fuego Core SML code

BASEDIR=../..
BUILD==build

include MakeVars

COASRC=message/util.sml message/stream.sml message/union-find.sml \
	message/tokenize.sml message/combinators.sml message/relax-ng.sml \
	message/rng-transform.sml message/coa-machine.sml message/print.sml \
	message/java-coa.sml message/coa.sml

CODECSRC=message/util.sml message/stream.sml message/tokenize.sml \
	message/combinators.sml message/orl.sml message/orl-extract.sml \
	message/codec.sml

all: $(BUILD)/bin/coa $(BUILD)/bin/codec

$(BUILD)/bin/coa: message/coa.cm $(COASRC)
	mkdir -p $(BUILD)/bin
	$(SML) $(SMLFLAGS) message/coa.cm
	mv message/coa $@

$(BUILD)/bin/codec: message/codec.cm $(CODECSRC)
	mkdir -p $(BUILD)/bin
	$(SML) $(SMLFLAGS) message/codec.cm
	mv message/codec $@

check: all
	@./check

clean:
	$(RM) -r $(BUILD)

.PHONY: all clean
