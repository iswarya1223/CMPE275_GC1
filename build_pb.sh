#!/bin/bash
#
# build the gRPC/protobuf (proto3) classes from the .proto. 
#
# NOTE this requires the java grpc codegen plugin
#

project_base="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

# generated code is put here
generated_base=${project_base}/generated

# define protoc to use - otherwise it uses your env path
#PROTOC_HOME=/opt/homebrew/Cellar/protobuf/21.5
PROTOC_HOME=/
# REALLY IMPORTANT: what/where is your java codegen plugin
#java_codegen=/usr/local/grpc/java-plugin-1.40.0/install/java_plugin/protoc-gen-grpc-java

# intel processors only (not apple m1)
#java_codegen=/Users/gash/Downloads/protoc-gen-grpc-java-1.44.1-osx-aarch_64.exe
java_codegen=/mnt/c/Users/iswar/Downloads/CMPE275/Lab1/protoc-gen-grpc-java-1.44.1-linux-x86_64.exe
if [ -d ${generated_base} ]; then
  echo -e "\n* removing contents of ${generated_base}"
  rm -r ${generated_base}/*
else
  echo -e "\n* creating directory ${generated_base}"
  mkdir ${generated_base}
fi

# for all .proto files in resources
for p in `ls ${project_base}/resources/*.proto`; do
   base=$(basename $p |cut -f1 -d'.')
   echo -e "\n* compiling $base"
   $PROTOC_HOME/bin/protoc \
        --proto_path=${project_base}/resources \
        --java_out=${generated_base} \
        --plugin=protoc-gen-grpc-java=${java_codegen} \
        --grpc-java_out=${generated_base} $p
done

echo -e "\n* done, created:\n"
find ${generated_base} -name "*.java" | xargs basename
echo -e "\n"

