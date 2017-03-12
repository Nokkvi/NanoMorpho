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
    final static int CALL = 1010;
    final static int STORE = 1011;
    final static int PAREN = 1012;


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
            code = program();
        }
        catch( Throwable e )
        {
            System.out.println(e.getMessage());
        }
        generateProgram(args[0],code);
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
            int variable = findVar(over(NAME));
            over('=');
            return new Object[]{"STORE", variable, expr()};
        }
        else
        {
        	return binopexpr(1);
        }
    }

    static Object[] binopexpr(int pri) throws Exception
    {
        if( pri>7 ){
            return smallexpr();
        }
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

    static Object[] smallexpr() throws Exception
    {
        Object[] res;
        Vector<Object> resu = new Vector<Object>();
        switch( getToken1() )
        {
        case NAME:
            String name = over(NAME);
            if( getToken1()=='(' )
            {
                over('(');
                if( getToken1()!=')' )
                {
                    for(;;)
                    {
                        resu.add(expr());
                        if( getToken1()==')' ) break;
                        over(',');
                    }
                }
                over(')');
                return new Object[]{"CALL", name, resu.toArray()};
            }
            return new Object[]{"NAME", findVar(name)};
        case WHILE:
            Object condition, whileExpr;
            over(WHILE);
            condition = expr();
            whileExpr = body();
            return new Object[]{"WHILE",condition,whileExpr};
        case IF:
            over(IF);
            Object[] a = expr();
            Object[] b = body();
            Vector<Object> elsif = new Vector<Object>();
            while( getToken1()==ELSIF )
            {
                over(ELSIF);
                elsif.add("ELSIF");
                elsif.add(expr());
                elsif.add(body());
            }
            Object[] c = null;
            if( getToken1()==ELSE )
            {
                over(ELSE);
                c = body();
            }
            return new Object[]{"IF", a, b, "ELSE", c, elsif.toArray()};
        case LITERAL:
            res = new Object[]{"LITERAL", getLexeme()};
            over(LITERAL);
            return res;
        case OPNAME:
            String opname = getLexeme();
            over(OPNAME);
            return new Object[]{"OPNAME", opname, smallexpr()} ;
        case '(':
            over('(');
            Object[] e = expr();
            over(')');
            return e;
        default:
            NanoMorphoLexer.expected("expression");
        }
        return null;
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
            throw new Error("Invalid opname at "+NanoMorphoLexer.getLine()+" Column: "+NanoMorphoLexer.getToken2()+"");
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
            //fun = {fname, argcount, varcount, res.toArray()};
            String fname = (String)fun[0];

            int argCount = (int)fun[1];
            int varCount = (int)fun[2];
            System.out.println("#\""+fname+"[f"+argCount+"]\" =");

            System.out.println("[");

            for(int k = 0; k<varCount;k++){
                System.out.println("(MakeVal null)");
                System.out.println("(Push)");
            }

            for(Object e:(Object[])fun[3]){
                generateExpr((Object[])e);
            }
            System.out.println("(Return)");
            System.out.println("];");
    }

    static int nextLab = 0;

    static void generateExpr( Object[] e )
    {
        switch((String)e[0]){
            case "NAME":
                System.out.println("(Fetch "+e[1]+")");
                return;
            case "LITERAL":
                System.out.println("(MakeVal "+(String)e[1]+")");
                return;
            case "RETURN":
                generateExpr((Object[])e[1]);
                System.out.println("(Return)");
                return;
            case "OPNAME":
                generateExpr((Object[])e[2]);
                System.out.println("(Call \""+e[1]+"[f1]\" "+1+")");
                return;
            case "IF":
                //e = {res, elsif, els}
                Object[] argu = (Object[])e[5];
                int labElse = nextLab++;
                int labEnd = nextLab++;
                generateExpr((Object[])e[1]);
                System.out.println("(GoFalse _"+labElse+")");
                generateBody((Object[])e[2]);
                System.out.println("(Go _"+labEnd+")");
                for(int i = 0; i<argu.length;i+=3){
                  System.out.println("_"+labElse+":");
                  generateExpr((Object[])argu[i+1]);
                  System.out.println("(GoFalse _"+labElse+")");
                  generateBody((Object[])argu[i+2]);
                  System.out.println("(Go _"+labEnd+")");
                }
                System.out.println("_"+labElse+":");
                generateBody((Object[])e[4]);
                System.out.println("_"+labEnd+":");
                return;
            case "WHILE":
                int labStart = nextLab++;
                int labQuit = nextLab++;
                System.out.println("_"+labStart+":");
                generateExpr((Object[])e[1]);
                System.out.println("(GoFalse _"+labQuit+")");
                generateBody((Object[])e[2]);
                System.out.println("(Go _"+labStart+")");
                System.out.println("_"+labQuit+":");
                return;
            case "CALL":
                //e = {"CALL", name, args[expr,...,expr]}
                Object[] args = (Object[])e[2];
                if( args.length!=0){
                  generateExpr((Object[])args[0]);
                }
                for (int i = 1; i!=args.length; i++){
                  System.out.println("(Push)");
                  generateExpr((Object[])args[i]);
                }
                System.out.println("(Call #\""+e[1]+"[f"+args.length+"]\" "+args.length+")");
                return;
            case "STORE":
                generateExpr((Object[])e[2]);
                System.out.println("(Store "+e[1]+")");
                return;
            case "PAREN":
                generateExpr((Object[])e[1]);
                return;

        }
    }

    static void generateBody( Object[] bod )
    {
		    for(int i=0; i<bod.length; i++) {
			       generateExpr((Object[])bod[i]);
        }
    }
}
