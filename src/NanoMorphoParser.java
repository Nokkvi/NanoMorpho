import java.util.Vector;
import java.util.HashMap;
import java.io.*;

public class NanoMorphoParser
{
    final static int ERROR = -1;
    final static int IF = 1001;
    final static int ELSE = 1002;
    final static int ELSIF = 1003;
    final static int WHILE = 1004;
    final static int VAR = 1005;
    final static int RETURN = 1006;
    final static int NAME = 1007;
    final static int OPNAME = 1008;
    final static int LITERAL = 1009;


    static String advance() throws Exception
    {
        return NanoMorphoLexer.advance();
    }

    static String getLexeme(){
    	return NanoMorphoLexer.getLexeme();
    }

    static String over( int tok ) throws Exception
    {
        return NanoMorphoLexer.over(tok);
    }

    static String over( char tok ) throws Exception
    {
        return NanoMorphoLexer.over(tok);
    }

    static int getToken1()
    {
        return NanoMorphoLexer.getToken1();
    }

    private static int varCount;
	private static HashMap<String,Integer> varTable;

	private static void addVar( String name )
	{
		if( varTable.get(name) != null )
			throw new Error("Variable "+name+" already exists, near line "+NanoMorphoLexer.getLine());
		varTable.put(name,varCount++);
	}

	private static int findVar( String name )
	{
		Integer res = varTable.get(name);
		if( res == null )
			throw new Error("Variable "+name+" does not exist, near line "+NanoMorphoLexer.getLine());
		return res;
	}

    static public void main( String[] args ) throws Exception
    {
    	Object[] code = null;
        try
        {
            NanoMorphoLexer.startLexer(args[0]);
            program();
        }
        catch( Throwable e )
        {
            System.out.println(e.getMessage());
        }
    }

    static Object[] program() throws Exception
    {
    	Vector<Object> res = new Vector<>();
    	while( getToken1()!=0 ) res.add(function());
        return res.toArray();
    }

    static Object[] function() throws Exception
    {
    	varCount = 0;
    	varTable = new HashMap<String,Integer>();

        String fname = over(NAME);

        over('(');
        if( getToken1()!=')' )
        {
            for(;;)
            {
            	addVar(over(NAME));
                if( getToken1()!=',' ) break;
                over(',');
            }
        }
        over(')'); over('{');
        int argCount = varCount;
        while( getToken1()==VAR )
        {
			decl();
            over(';');
        }

		Vector<Object> res = new Vector<Object>();

        while( getToken1()!='}' )
        {
            res.add(expr());
            over(';');
        }
        over('}');

        return new Object[]{fname, argCount, varCount-argCount, res.toArray()};
    }

    static void decl() throws Exception
    {
    	over(VAR);
        for(;;)
        {
            addVar(over(NAME));
            if( getToken1()!=',' ) break;
            over(',');
        }
    }

    static Object[] expr() throws Exception
    {
        if( getToken1()==RETURN )
        {
            over(RETURN);
            return new Object[]{"RETURN", expr()};
        }
        else if( getToken1()==NAME && NanoMorphoLexer.getToken2()=='=' )
        {
            Integer variable = findVar(over(NAME));
            over('=');
            return new Object[]{variable, '"', expr()};
        }
        else
        {
        	return binopexpr();
        }
    }

    static Object[] binopexpr(int pri) throws Exception
    {
        if( pri>7 )
            return smallexpr();
        else if( pri==2 )
        {
            Object[] e = binopexpr(3);
            if( getToken1()==OPNAME && priority(NanoMorphoLexer.getLexeme())==2 )
            {
                String op = advance();
                e = new Object[]{"CALL",op,new Object[]{e,binopexpr(2)}};
            }
            return e;
        }
        else
        {
            Object[] e = binopexpr(pri+1);
            while( getToken1()==OPNAME && priority(NanoMorphoLexer.getLexeme())==pri )
            {
                String op = advance();
                e = new Object[]{"CALL",op,new Object[]{e,binopexpr(pri+1)}};
            }
            return e;
        }
    }
    //TODO: kl�ra
    static Object[] smallexpr() throws Exception
    {
        Object[] res;
        switch( getToken1() )
        {
        case NAME:
            String name = over(NAME);
            if( getToken1()=='(' )
            {
                over('(');
                if( getToken1()!=')' )
                {
                    Vector<Object> resu = new Vector<Object>();
                    for(;;)
                    {
                        res.add(expr());
                        if( getToken1()==')' ) break;
                        over(',');
                    }
                }
                over(')');
                return new Object[]{"CALL", name, resu.toArray()};
            }
            return new Object[]{"NAME", name};
        case WHILE:
            Object condition, whileExpr;
            over(WHILE);
            condition = expr();
            whileExpr = body();
            return new Object[]{"WHILE",condition,whileExpr};
        case IF:
            over(IF);
            res.add("IF");
            res.add(expr());
            res.add(body());
            while( getToken1()==ELSIF )
            {
                over(ELSIF);
                res.add("ELSIF");
                res.add(expr());
                res.add(body());
            }
            if( getToken1()==ELSE )
            {
                over(ELSE);
                res.add("ELSE");
                res.add(body());
            }
            return res;
        case LITERAL:
            res = new Object[]{"LITERAL", getToken1()};
            over(LITERAL);
            return res;
        case OPNAME:
            res.add("OPNAME");
            res.add(getToken1());
            over(OPNAME);
            res.add(smallexpr());
            return res;
        case '(':
            over('(');
            res.add(expr());
            over(')');
            return res;
        default:
            NanoMorphoLexer.expected("expression");
        }
    }

    static Object[] body() throws Exception
    {
    	Vector<Object> res = new Vector<Object>();

        over('{');
        while( getToken1()!='}' )
        {
            res.add(expr());
            over(';');
        }
        over('}');

        return res.toArray();
    }

    static int priority( String opname )
    {
        switch( opname.charAt(0) )
        {
        case '^':
        case '?':
        case '~':
            return 1;
        case ':':
            return 2;
        case '|':
            return 3;
        case '&':
            return 4;
        case '!':
        case '=':
        case '<':
        case '>':
            return 5;
        case '+':
        case '-':
            return 6;
        case '*':
        case '/':
        case '%':
            return 7;
        default:
            throw new Error("Invalid opname");
        }
    }

    static void generateProgram( String filename, Object[] funs )
    {
        String programname = filename.substring(0,filename.indexOf('.'));
        System.out.println("\""+programname+".mexe\" = main in");
        System.out.println("!");
        System.out.println("{{");
        for( Object f: funs )
        {
            generateFunction((Object[])f);
        }
        System.out.println("}}");
        System.out.println("*");
        System.out.println("BASIS;");
    }

    static void generateFunction( Object[] fun )
    {
            //fun = {fname, countount, varcount, res.toArray()};
            String fname = (String)fun[0];
            int count = (Integer)fun[1];
            int varcount = (Integer)fun[2]; 
            System.out.println("#\""+fname+"[fun"+count+"]\" =");
            System.out.println("[");

            for(int k = 0; k<varcount;k++){
                System.out.println("(MakeVal null)");
                System.out.println("(Push)");
            }

            for(int i=3; i!=fun.length; i++) generateExpr((Object)fun[i]);
            System.out.println("Return");
            System.out.println("];");
    }

    static int nextLab = 0;

    static void generateExpr( Object[] e )
    {

    }

    static void generateBody( Object[] bod )
    {
		for(int i=0; i<bod.length; i++) {
			generateExpr((Object[])bod[i]);
        }
    }
}