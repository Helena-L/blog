package blog.absyn;

/**
 * abstract syntax node for Array type
 * 
 * @author leili
 * @date Apr 22, 2012
 * 
 */
public class ArrayTy extends Ty {
	public Ty typ;
	public int dim;

	public ArrayTy(int p, Ty t, int d) {
		pos = p;
		typ = t;
		dim = d;
	}

	@Override
	public void printTree(Printer pr, int d) {
		pr.indent(d);
		pr.say("ArrayTy(");
		typ.printTree(pr, d + 1);
		pr.sayln(",");
		pr.indent(d + 1);
		pr.say(dim);
		pr.say(")");
	}
}
