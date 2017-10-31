package edu.nyu.oop;

import org.slf4j.Logger;
import xtc.tree.GNode;
import xtc.tree.Node;
import xtc.tree.Printer;
import xtc.tree.Visitor;

import edu.nyu.oop.util.XtcProps;
import edu.nyu.oop.util.NodeUtil;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;

/* Print out the information in the AST in a concrete C++ syntax generated from Phase 4
 * Under construction, working for most classes and methods
 * currently working on the seperation of main function
 * no indent
 */

public class Phase5 extends Visitor {

    private Logger logger = org.slf4j.LoggerFactory.getLogger(this.getClass());

    private  Printer printer;

    private String outputLocation = XtcProps.get("output.location");

    String packageInfo = "";

    /* Class constructor. Intializing the writer to the file. */
    public Phase5() {

        Writer w = null;
        try {
            FileOutputStream fos = new FileOutputStream(outputLocation + "/output.cpp");
            OutputStreamWriter ows = new OutputStreamWriter(fos, "utf-8");
            w = new BufferedWriter(ows);
            this.printer = new Printer(w);
        } catch (Exception e) {
            throw new RuntimeException("Output location not found. Create the /output directory.");
        }

        printer.register(this);
    }

    /* The actual print method */
    public void print(GNode ast) {
        headOfFile();
        dispatch(ast);
        printer.flush();
    }

    /* The claim placed in the beginning of cpp files */
    private void headOfFile() {
        printer.pln("#include <stdint.h>");
        printer.pln("#include <string>");
        printer.pln("#include \"output.h\"");
        printer.pln().pln();
    }

    /* Visitor for ClassDeclaration 
     * Generate code for default constructor, Class method(class identity and its parent)
     * and vtable initialization.
     */
    public void visitClassDeclaration(GNode n) {

        //Obtaining information of class name of parent class name
        String className = n.get(1).toString();
        String parentName = "";
        GNode extensionClass = (GNode) NodeUtil.dfs(n, "Extension");
        if (extensionClass != null) {
            GNode parentType = (GNode) extensionClass.get(0);
            GNode parentTypeNode = (GNode) parentType.get(0);
            parentName = parentTypeNode.get(0).toString();
        } else  {
            parentName = "Object";
        }

        //default constructor
        printer.pln("__" + n.get(1).toString() + "::__" + n.get(1).toString() + "() : __vptr(&__vtable) {}" );
        printer.pln().flush();

        //visit class body
        GNode classBody = (GNode) NodeUtil.dfs(n, "ClassBody");
        dispatch(classBody);

        //class method
        printer.pln("Class __" + n.get(1).toString() + "::__class() {");
        printer.pln("static Class k = new __class(__rt::literal(\""
                    + packageInfo + className + "\"), __"
                    + parentName + "::__class());");
        printer.pln("return k;");
        printer.pln("}");
        printer.pln();

        //vtable initialization
        printer.pln("__" + className + "_VT __" + className
                    + "::__vtable;");
        printer.pln();
    }

    public void visitConditionalStatement(GNode n) {
        printer.p("if (").flush();
        GNode condition = (GNode) n.get(0);
        dispatch(condition);
        printer.p(")").flush();

        for (int i = 1; i < n.size(); i++) {
            if (n.get(0) instanceof Node) dispatch((Node) n.get(i));
            if (n.get(0) instanceof String) printer.p((String) n.get(i) + " ").flush();
        }
    }

    public void visitInstanceOfExpression(GNode n) {
        dispatch((Node) n.get(0));
        printer.p("-> __vptr -> instanceof(").flush();
        dispatch((Node) n.get(0));
        printer.p(", (Object) new __").flush();
        dispatch((Node) n.get(1));
        printer.p("())").flush();

    }

    /* Visitor for CompilationUnit 
     * Generate information of the package and print namespace
     */
    public void visitCompilationUnit(GNode n) {

        //print namespace and generate package info
        GNode p = (GNode) n.getGeneric(0);
        GNode packageName = (GNode) p.getGeneric(1);
        for (int i = 0; i < packageName.size(); i ++) {
            packageInfo += packageName.get(i).toString() + ".";
            printer.pln("namespace " + packageName.get(i).toString()).flush();
            printer.pln("{").flush();
        }

        //visit children(except for the 0-index node -- which is package name)
        for (int i = 1; i < n.size(); i++) {
            Object o = n.get(i);
            if (o instanceof Node) dispatch((Node) o);
        }

        //right brackets
        for (int i = 0; i < packageName.size(); i ++) {
            printer.pln("}").flush();
        }
        printer.pln().flush();
    }

    /* Visitor for FieldDeclaration 
     * Add ";" at the end of each statement
     */
    public void visitFieldDeclaration(GNode n) {
        visit(n);
        printer.pln(";").flush();
    }

    /* Visitor for Block 
     * Add brackets "{}" at beginning and ending
     */
    public void visitBlock(GNode n) {
        printer.pln("{").flush();
        visit(n);
        printer.pln("}").flush();
        printer.pln().flush();

    }

    /* Visitor for Arguments 
     * Add brackets "()" at beginning and ending
     * add comma between elements
     */
    public void visitArguments(GNode n) {

        printer.p("(").flush();

        //add comma
        for (int i = 0; i < n.size() - 1; i++) {
            try {
                GNode child = (GNode) n.getGeneric(i);
                dispatch(child);
                printer.p(", ").flush();
            } catch (Exception e) {}
        }

        try{
            Object child = n.getGeneric(n.size() - 1);
            if (child instanceof GNode) {
                dispatch((GNode) child);       
            }
            else if (child instanceof String) {
                printer.p((String) child).flush();
            }
        }
        catch (Exception e) {}
        printer.p(")").flush();
    }

    /* Visitor for FormalParameters 
     * Same as Arguments
     */
    public void visitFormalParameters(GNode n) {
        visitArguments(n);
    }

    /* Skip extension since we already have its info */
    public void visitExtension(GNode n) {}

    /* Visitor for CastExpression
     * Add "()" to the cast type
     */
    public void visitCastExpression(GNode n) {

        printer.p("(").flush();
        GNode type = (GNode) n.getGeneric(0);

        dispatch(type);

        printer.p(") ").flush();

        dispatch((GNode) n.getGeneric(1));
    }

    /* Visitor for CallExpression
     * For cout, arguments should be placed after "<<"" instead of inside "()"
     * toString() should be tranformed to to toString() -> data
     */
    public void visitCallExpression(GNode n) {

        //cout handling
        if (n.get(2).toString().equals("cout")) {
            printer.p("cout ").flush();
            GNode arguments = (GNode) n.getGeneric(3);

            //print arguments, starts with "<<"
            for (Object o : arguments) {
                printer.p("<< ").flush();
                if (o instanceof Node) {
                    GNode gnode = (GNode) o;
                    dispatch((Node) o);

                    //toString handling
                    if (gnode.hasName("CallExpression")) {
                        if (gnode.get(2).toString().endsWith("toString")) {
                            printer.p("-> data ").flush();
                        }
                    }
                }

                if (o instanceof String) printer.p(((String) o) + " ").flush();
            }
        }

        //other calling will not be specialized.
        else {
            visit(n);
        }
    }

    /* Visitor for ExpressionStatement
     * Add ";" at the end of statement
     */
    public void visitExpressionStatement(GNode n) {
        visit(n);
        printer.pln(";").flush();
    }

    /* Visitor for ReturnStatement
     * print return, ending with ";"
     */
    public void visitReturnStatement(GNode n) {
        printer.p("return ").flush();
        visit(n);
        printer.pln(";").flush();
    }

    /* Visitor for VoidType
     * print "void"
     */
    public void visitVoidType(GNode n) {
        printer.p("void ").flush();
    }

    /* Visitor for Declarator
     * adding "=" to the statement
     */
    public void visitDeclarator(GNode n) {
        printer.p(n.get(0).toString() + " ").flush();
        for (int i = 1; i < n.size(); i++) {
            try {
                GNode child = (GNode) n.getGeneric(i);
                if (child != null) {
                    printer.p("= ").flush();
                    dispatch(child);
                }
            } catch (Exception e) {}
        }

    }
    
    /* Visitor for NewClassExpression
     * print "new"
     */
    public void visitNewClassExpression(GNode n) {
        printer.p("new ").flush();
        visit(n);
    }

    /* Visitor for ThisExpression
     * print "__this"
     */
    public void visitThisExpression(GNode n) {
        printer.p("__this ").flush();
        visit(n);
    }

    /* General visitor
     * Besides dispatch, also print all String instances it meets
     */
    public void visit(Node node) {
        for (Object o : node) {

            //dispatch
            if (o instanceof Node) dispatch((Node) o);

            //print string
            if (o instanceof String) {
                String s = (String) o;
                if (s.equals("[")) {
                    printer.p("[] ").flush();
                } else {
                    printer.p(s + " ").flush();
                }
            }
        }
    }
}