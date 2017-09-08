#!/bin/bash
FILES=Android/*
for f in $FILES
do
  if [[ $f != *"android--1"* ]]
  then
	for subf in $f/*
	do
	   if [[ $subf == *".apk"* ]]
   	   then
	     java -jar SQLUsage.jar Android $f /classlist.txt ${subf/$f/""} api.txt
	     java -jar SQLString.jar Android $f /classlist.txt ${subf/$f/""} string.txt
	     java -jar SQLTransactionAll.jar Android $f /classlist.txt ${subf/$f/""} apiInLoopTransaction.txt

	   fi


        done

  fi
done
