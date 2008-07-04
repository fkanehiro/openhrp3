#!/bin/bash

. config.sh
if [ -f $OPENHRPHOME/bin/unix/omninames-$HOSTNAME.log ] ; then
    /bin/rm $OPENHRPHOME/bin/unix/omninames-$HOSTNAME.*
fi
if [ $1 ]; then
echo $NAMESERV
else
exec $NAMESERV
fi



