NanoMorphoLexer.class NanoMorphoParser.class: NanoMorphoLexer.java NanoMorphoParser.java
	javac NanoMorphoLexer.java NanoMorphoParser.java
NanoMorphoLexer.java: nanomorpholexer.jflex
	java -jar jflex-1.6.1.jar nanomorpholexer.jflex
clean:
	rm -Rf *~ *.class NanoMorphoLexer.java *.masm *.mexe
test: NanoMorphoLexer.class NanoMorphoParser.class test.nm
	java NanoMorphoParser test.nm