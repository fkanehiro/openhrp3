
. config.sh

cd $OPENHRPHOME/PositionSensor/server
while : ; do
    ./PositionSensor $NS_OPT
done
