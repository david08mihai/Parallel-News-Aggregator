JC = javac
JVM = java
MAIN = NewsAggregator
LIBS = libs

CP = .:$(LIBS)/*
JFLAGS = -g -cp "$(CP)"

THREADS ?= 4

.PHONY: build clean run test1 test2

build:
	$(JC) $(JFLAGS) *.java

run:
	$(JVM) -cp "$(CP)" $(MAIN) $(ARGS)

test1: build
	$(JVM) -cp "$(CP)" $(MAIN) $(THREADS) ../tests/input/tests/test_1/articles.txt ../tests/input/tests/test_1/inputs.txt

test2: build
	$(JVM) -cp "$(CP)" $(MAIN) $(THREADS) ../tests/input/tests/test_2/articles.txt ../tests/input/tests/test_2/inputs.txt

test3: build
	$(JVM) -cp "$(CP)" $(MAIN) $(THREADS) ../tests/input/tests/test_3/articles.txt ../tests/input/tests/test_3/inputs.txt

test4: build
	$(JVM) -cp "$(CP)" $(MAIN) $(THREADS) ../tests/input/tests/test_4/articles.txt ../tests/input/tests/test_4/inputs.txt

test5: build
	$(JVM) -cp "$(CP)" $(MAIN) $(THREADS) ../tests/input/tests/test_5/articles.txt ../tests/input/tests/test_5/inputs.txt



clean:
	rm -f *.class
	rm *.txt