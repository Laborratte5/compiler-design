#!/bin/bash

BASEFOLDER=$(dirname "$PWD")
INPUT_FILES=$(find "$BASEFOLDER" -type f -name "*.c0")

"$BASEFOLDER"/build.sh
echo "$INPUT_FILES"

echo
echo TESTING
echo

for f in $INPUT_FILES; do
    out_file=$f.s
    expected_file=$f.expected
    echo -n Testing "$(basename \""$f")" "$(basename \""$out_file")"
    "$BASEFOLDER"/run.sh "$f" "$out_file" &> /dev/null
    if test $? -ne 0; then
        echo " FAILED <Compiler error>"
        continue
    fi
    diff -q "$out_file" "$expected_file" > /dev/null
    if test $? -eq 0; then
        echo " PASSED"
        rm "$out_file"
    else
        echo " FAILED <Invalid result>"
        diff "$out_file" "$expected_file"
    fi
done
