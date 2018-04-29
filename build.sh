echo "Building..."
if [ -d "bin" ]; then
  rm -rf bin/*
  rmdir bin
fi

cd src
find $PWD | grep '\.groovy' > ../tmpsources.txt
cd ..
mkdir bin
groovyc -cp "./lib/*" -d bin @tmpsources.txt
cp store.jks bin/
rm tmpsources.txt

echo "Done!"
