task = compiler analyzer translator assembler
main : first $(task)

first :
	@if [ ! -d bin ]; then mkdir bin; fi
compiler : i = JackCompiler
analyzer : i = JackAnalyzer
translator : i = VMTranslator
assembler : i = Assembler
$(task): 
	@kotlinc -nowarn ./$i/src/*.kt -include-runtime -d ./bin/$i.jar
	echo "java -Dfile.encoding=UTF-8 -jar ./bin/$i.jar \$$1" > ./bin/$i
	@jar --update --file ./bin/$i.jar --main-class MainKt
	@chmod +x ./bin/$i
.PHONY : clean
clean :
	@-rm -rf ./bin