#!/bin/sh

ruby make-src-archive.rb OpenHRP-$1 OpenHRP3-src-exclude-list --script OpenHRP3-src-arrange.sh --use-zip
