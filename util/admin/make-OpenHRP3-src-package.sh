#!/bin/sh

if [ -z $2 ]; then
  ruby make-src-archive.rb OpenHRP-$1 OpenHRP3-src-exclude-list --script OpenHRP3-src-arrange.sh --use-zip
else
  ruby make-src-archive.rb OpenHRP-$1 OpenHRP3-src-exclude-list --script OpenHRP3-src-arrange.sh --use-zip --svn-path $2
fi
