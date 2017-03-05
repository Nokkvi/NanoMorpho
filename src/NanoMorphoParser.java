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
    	Vector<Object> collect = new Vector<>();
    	while( getToken1()!=0 ) collect.add(function());
        return collect.toArray();
    }

    static Object[] function() throws Exception
    {
    	varCount = 0;
    	Vector<String> args = new Vector<String>();
    	Vector<Object> res = new Vector<>();
    	varTable = new HashMap<String,Integer>();
    	res.add(getLexeme());
    	addVar(getLexeme());
        args.add(over(NAME)); 
    
        over('(');
        if( getToken1()!=')' )
        {
            for(;;)
            {
            	addVar(getLexeme());
            	args.add(over(NAME)); 
                if( getToken1()!=',' ) break;
                over(',');
            }
        }
        over(')'); over('{');
        int argCount = args.size()-1;
        while( getToken1()==VAR )
        {
            varCount += decl(); 
            over(';');
        }
        
        res.add(argCount);
        res.add(varCount);
        
        res.add(expr());
        
        while( getToken1()!='}' )
        {
            res.add(expr()); 
            over(';');
        }
        over('}');
        
        return res.toArray();
    }

    static int decl() throws Exception
    {
        int varcount = 1;
    	over(VAR);
        for(;;)
        {
            addVar(over(NAME));
            varcount++;
            if( getToken1()!=',' ) break;
            over(',');
        }
        return varcount;
    }

    static Object[] expr() throws Exception
    {
    	Vector<Object> res = new Vector<>();
        if( getToken1()==RETURN )
        {
            over(RETURN); 
            res.add(expr());
        }
        else if( getToken1()==NAME && NanoMorphoLexer.getToken2()=='=' )
        {
            findVar(over(NAME)); 
            over('='); 
            res.add(expr());
        }
        else
        {
        	res.add(binopexpr());
        }
        return res.toArray();
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
    //TODO: klára
    static Object[] smallexpr() throws Exception
    {
        switch( getToken1() )
        {
        case NAME:
            over(NAME);
            if( getToken1()=='(' )
            {
                over('(');
                if( getToken1()!=')' )
                {
                    for(;;)
                    {
                        expr();
                        if( getToken1()==')' ) break;
                        over(',');
                    }
                }
                over(')');
            }
            return;
        case WHILE:
            over(WHILE); expr(); body(); return;
        case IF:
            over(IF); expr(); body();
            while( getToken1()==ELSIF )
            {
                over(ELSIF); expr(); body();
            }
            if( getToken1()==ELSE )
            {
                over(ELSE); body();
            }
            return;
        case LITERAL:
            over(LITERAL); return;
        case OPNAME:
            over(OPNAME); smallexpr(); return;
        case '(':
            over('('); expr(); over(')'); return;
        default:
            NanoMorphoLexer.expected("expression");
        }
    }

    static Object[] body() throws Exception
    {
    	Vector<Object> res = new Vector<>();
    	
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
		
    }
    
    static int nextLab = 0;
    
    static void generateExpr( Object[] e )
    {
		
    }
    
    static void generateBody( Object[] bod )
    {
		
    }
}