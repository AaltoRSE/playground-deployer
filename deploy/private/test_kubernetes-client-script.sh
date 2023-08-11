#!/bin/bash
PATH_SOLUTION_ZIP=***
SECRET_NAME=***
NAMESPACE=***
PATH_SECRET=***

kubectl delete ns $NAMESPACE
kubectl create ns $NAMESPACE

rm -r  ${PATH_SOLUTION_ZIP}/test
mkdir ${PATH_SOLUTION_ZIP}/test
unzip  ${PATH_SOLUTION_ZIP}/solution.zip -d ${PATH_SOLUTION_ZIP}/test

python3 kubernetes-client-script.py  -n test -bp ${PATH_SOLUTION_ZIP}/test -sn $SECRET_NAME -ps $PATH_SECRET