#!/bin/sh

VERSION="0.1-SNAPSHOT"
SCALA_VERSION="2.12.3"

echo "Do you wish to use the current directory for the project root? (y/n)?"
echo "Note: If (n) then the project will be created in a sub-directory of the current one."

read answer

if echo "$answer" | grep -iq "^y" ;then
    PROJECT_DIR="."
    PROJECT_NAME=${PWD##*/}
else
    echo "Provide the project name (the root directory will be named the same)."
    read PROJECT_DIR
    PROJECT_NAME=$PROJECT_DIR
    mkdir $PROJECT_DIR
fi

if [ $PROJECT_DIR == "." ] ;then
    echo "Project will take name of current directory -> " $PROJECT_NAME
else
	echo "Creating SBT project in" $PROJECT_DIR
fi

mkdir -p $PROJECT_DIR/src/main/scala
mkdir -p $PROJECT_DIR/src/test/scala

# Add a sample source file
#echo 'object Hi { def main(args: Array[String]) = println("Hi!") }' > $PROJECT_DIR/src/main/scala/hello.scala

# Create Build definition
echo 'name := "'$PROJECT_NAME'"\n\nversion := "'$VERSION'"\n\nscalaVersion := "'$SCALA_VERSION'"' >> $PROJECT_DIR/build.sbt

# Create plugins.sbt
mkdir -p $PROJECT_DIR/project
echo '// Declare plugins below\n' >> $PROJECT_DIR/project/plugins.sbt

cd $PROJECT_DIR
sbt ";update; exit"