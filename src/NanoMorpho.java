import java.io.*;
import java.util.*;

public class NanoMorpho {
	private static NanoMorphoLexer lexer;
	private static int next_token;
	private static String lexeme;
	
	enum CodeType {
        NAME, ASSIGN, CALL, RETURN, OP, LITERAL, IF, WHILE, ELSE, PRIOR
    };
	
}
