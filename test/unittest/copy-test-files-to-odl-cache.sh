
mkdir -p ../../sdnmud-aggregator/karaf/target/assembly/etc/mudprofiles/
target=../../sdnmud-aggregator/karaf/target/assembly/etc/mudprofiles
rm  $target/*
cp mud-local-and-same-mfg-test/mudfile-local-and-same-mfg-test.json  $target
cp mud-local-and-same-mfg-test1/mudfile-local-and-same-mfg-test1.json  $target
cp mud-same-manufacturer-test/mudfile-same-manufacturer-test.json  $target
cp mud-mfg-test/mudfile-mfg-test.json  $target
cp mud-local-networks-test/mudfile-local-networks-test.json $target
cp mud-tcp-direction-test/mudfile-tcp-direction-test.json $target
cp mud-model-test/mudfile-model-test.json $target
cp mud-same-mfg-scale-test/mudfile-same-mfg-scale-test.json $target
cp mud-controllerclass-test/mudfile-controllerclass-test.json $target