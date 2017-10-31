package edu.nyu.oop;

import org.slf4j.Logger;
import xtc.tree.GNode;
import xtc.tree.Node;
import xtc.tree.Visitor;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.HashSet;

public class Phase2 {

    /**
     * Master method to run other methods
     * @param n root node of given AST parsed by Phase 1
     * @return root node of AST with built layout and structure for each child and itself
     */
    public static Node runPhase2(Node n) {

        // this was for printing contents of data structures before node processing, now that portion of the code has been commented out
        boolean dump = false;

        //Traverse Java AST
        Phase2Visitor visitor = new Phase2Visitor();
        visitor.traverse(n);

        //Build list of class representations (java.lang, inheritance)
        ObjectRepList unfilled = visitor.getObjectRepresentations();
        ObjectRepList filled = getFilledObjectRepList(unfilled);

        //Build C++ AST from class representations
        return buildCppAst(visitor.getPackageName(), filled);
    }

    /**
     * Visitor class
     */
    public static class Phase2Visitor extends Visitor {

        private Logger logger = org.slf4j.LoggerFactory.getLogger(this.getClass());

        private String packageName = "";
        private ObjectRepList objectRepresentations = new ObjectRepList();

        /**
         * Visits Package Declaration and assigns packagename
         * @param node node being visited
         */
        public void visitPackageDeclaration(GNode node){
            packageName = node.getNode(1).getString(0);
            visit(node);
        }

        /**
         * For each class declaration in file add it to objectRepresentations
         * @param node node being visited
         */
        public void visitClassDeclaration(GNode node) {
            objectRepresentations.add(new ObjectRep(node.getString(1)));
            visit(node);
        }

        /**
         * Adds inheritance of each node to structure
         * @param node node being visited
         */
        public void visitExtension(GNode node) {
            objectRepresentations.getCurrent().parent = new ObjectRep(node.getNode(0).getNode(0).getString(0));
            visit(node);
        }

        /**
         * Gets class constructor and adds it to node's AST
         * @param node node being visited
         */
        public void visitConstructorDeclaration(GNode node) {
            // modifiers
            String accessModifier = "";
            boolean isStatic = false;

            Iterator modifierIter = node.getNode(0).iterator();
            while (modifierIter.hasNext()) {
                Node modifierNode = (Node) modifierIter.next();
                if (modifierNode.getString(0).equals("static")) isStatic = true;
                else accessModifier = modifierNode.getString(0);
            }

            // name
            String constructorName = node.getString(2);

            // parameters
            ArrayList<Parameter> parameters = new ArrayList<Parameter>();

            parameters.add(new Parameter(objectRepresentations.getCurrent().name, "__this"));

            Iterator parameterIter = node.getNode(3).iterator();
            while (parameterIter.hasNext()) {
                Node parameterNode = (Node) parameterIter.next();
                String parameterType = convertType(parameterNode.getNode(1).getNode(0).getString(0));
                String parameterName = parameterNode.getString(3);
                parameters.add(new Parameter(parameterType, parameterName));
            }

            // add
            // constructorName should be called init, updating here
            Constructor constructor = new Constructor(accessModifier, "init", parameters);
            objectRepresentations.getCurrent().classRep.constructors.add(constructor);

            visit(node);
        }

        /**
         * Visits method declaration and adds it to current node's AST
         * @param node node being visited
         */
        public void visitMethodDeclaration(GNode node) {
            // modifiers
            String accessModifier = "";
            boolean isStatic = false;

            Iterator modifierIter = node.getNode(0).iterator();
            while (modifierIter.hasNext()) {
                Node modifierNode = (Node) modifierIter.next();
                if (modifierNode.getString(0).equals("static")) isStatic = true;
                else accessModifier = modifierNode.getString(0);
            }

            // return type
            String returnType;
            Node returnNode = node.getNode(2);
            if (returnNode.getName().equals("VoidType")) returnType = "void";
            else returnType = convertType(returnNode.getNode(0).getString(0));

            // name
            String methodName = node.getString(3);

            // parameters
            ArrayList<Parameter> parameters = new ArrayList<Parameter>();
            parameters.add(new Parameter(objectRepresentations.getCurrent().name, ""));

            Iterator parameterIter = node.getNode(4).iterator();
            while (parameterIter.hasNext()) {
                Node parameterNode = (Node) parameterIter.next();
                String parameterType = convertType(parameterNode.getNode(1).getNode(0).getString(0));
                String parameterName = parameterNode.getString(3);
                parameters.add(new Parameter(parameterType, parameterName));
            }

            // add
            Method method = new Method(accessModifier, isStatic, returnType, methodName, parameters);
            objectRepresentations.getCurrent().classRep.methods.add(method);

            visit(node);
        }

        /**
         * Visits field declaration and adds it to current node's AST
         * @param node node being visited
         */
        public void visitFieldDeclaration(GNode node) {
            // modifiers
            String accessModifier = "";
            boolean isStatic = false;

            Iterator modifierIter = node.getNode(0).iterator();
            while (modifierIter.hasNext()) {
                Node modifierNode = (Node) modifierIter.next();
                if (modifierNode.getString(0).equals("static")) isStatic = true;
                else accessModifier = modifierNode.getString(0);
            }

            // type
            String fieldType = convertType(node.getNode(1).getNode(0).getString(0));

            // name
            String fieldName = node.getNode(2).getNode(0).getString(0);

            // initial
            String initial = "";
            Node initial_node = node.getNode(2).getNode(0).getNode(2);

            // add
            Field field = new Field(accessModifier, isStatic, fieldType, fieldName, initial);
            objectRepresentations.getCurrent().classRep.fields.add(field);

            visit(node);
        }

        /**
         * Iteratively visit all children of given node
         * @param node root of current node Ast
         */
        public void visit(Node node) {
            for (Object o : node) if (o instanceof Node) dispatch((Node) o);
        }

        /**
         * Dispatch
         * @param node node being visited
         */
        public void traverse(Node node) {
            super.dispatch(node);
        }

        /**
         * Converts from Java type to C++ type
         * @param type java type
         * @return cpp type
         */
        public String convertType(String type) {
            if (type.equals("long")) return "int64_t";
            else if (type.equals("int")) return "int32_t";
            else if (type.equals("short")) return "int16_t";
            else if (type.equals("byte")) return "int8_t";
            else if (type.equals("boolean")) return "bool";
            else return type;
        }

        /**
         * Get package name
         * @return packageName
         */
        public String getPackageName() {
            return packageName;
        }

        /**
         * Gets ObjectRepList objectRepresentations
         * @return objectRepresentations
         */
        public ObjectRepList getObjectRepresentations() {
            return objectRepresentations;
        }
    }

    /**
     * ArrayList of Structure ObjectRep
     */
    public static class ObjectRepList extends ArrayList<ObjectRep> {

        /**
         * Method to get ObjectRep in last position of Array
         * @return ObjectRepList[-1]
         */
        public ObjectRep getCurrent() {
            return this.get(this.size() - 1);
        }

        /**
         * Iterativery searches for node by given name
         * @param name name to be searched
         * @return ObjectRep if in List, null if not in List
         */
        public ObjectRep getFromName(String name) {
            for (ObjectRep rep : this) if (rep.name.equals(name)) return rep;
            return null;
        }

        /**
         * Gets index of ObjectRep by its name
         * @param name name to be searched
         * @return index of ObjectRep if in List, -1 if not
         */
        public int getIndexFromName(String name) {
            int index = 0;
            for (ObjectRep rep : this)
                if (rep.name.equals(name))
                    return index;
                else index++;
            return -1;
        }
    }

    /**
     * Fills up structure with assigned methods VTables, parents
     * @param unfilled ObjectRepList from visitor
     * @return
     */
    public static ObjectRepList getFilledObjectRepList(ObjectRepList unfilled) {

        // manually add object, string, class
        ObjectRepList filled = initializeRepList();

        // fill with reps, in inheritance order
        filled = fill(filled, unfilled);

        // process reps
        for (ObjectRep rep : filled) {
            if (!rep.name.equals("Object") && !rep.name.equals("String") && !rep.name.equals("Class")) {
                // check if main is in the rep
                boolean main = false;
                for (Method method : rep.classRep.methods) if (method.name.equals("main")) main = true;
                // if main is not there, then process output and replace old with the new
                if (!main) {
                    ObjectRep newRep = processVTable(rep, rep.parent);
                    int index = filled.getIndexFromName(newRep.name);
                    filled.set(index, newRep);
                }
            }

            // don't forget to update the parents after replacing so that logic works, "bubbling down"
            for (ObjectRep repSub : filled) {
                if (repSub.parent != null) {
                    int parentIndex = filled.getIndexFromName(repSub.parent.name);
                    repSub.parent = filled.get(parentIndex);
                }
            }
        }

        // after processing v-table process inherited fields (this is the last step, everything else should be consistent)
        int counter = 0;
        for (ObjectRep rep : filled) {
            if (counter < 2) counter++;
            else {
                // as long as the parent isn't object there are fields that may be inherited (this depends on if static and so on)
                if (!rep.parent.name.equals("Object")) {
                    ObjectRep newRep = processFields(rep, rep.parent);
                    int index = filled.getIndexFromName(rep.name);
                    filled.set(index, newRep);
                }
            }
            // again, don't forget to update the parents after replacing so that logic works, "bubbling down"
            for (ObjectRep repSub : filled) {
                if (repSub.parent != null) {
                    int parentIndex = filled.getIndexFromName(repSub.parent.name);
                    repSub.parent = filled.get(parentIndex);
                }
            }
        }

        // remove rep with main method
        int mainIndex = -1;
        for (ObjectRep rep : filled) {
            for (Method method : rep.classRep.methods) {
                if (method.name.equals("main"))
                    mainIndex = filled.indexOf(rep);
            }
        }
        if(mainIndex != -1) filled.remove(mainIndex);

        // remove Object, String, and Class
        filled.remove(0);
        filled.remove(0);
        filled.remove(0);

        return filled;
    }

    /**
     * Helper method for getFilledObjectRepList that fills up the ObjectRep structure
     * @param filled
     * @param unfilled
     * @return filled structure
     */
    public static ObjectRepList fill(ObjectRepList filled, ObjectRepList unfilled) {

        //Add classes from unfilled, keep doing this until filled has same size as unfilled
        while (filled.size() < unfilled.size() + 3) {

            for (ObjectRep rep : unfilled) {

                // if no parent set parent to object's rep, which will be at filled.get(0)
                if (rep.parent == null) {
                    rep.parent = filled.get(0);
                }

                // don't add until parent class is already in filled, this makes sure classes are in inheritance order, i.e. if a class inherits a class it should be further down the list
                if (filled.getFromName(rep.parent.name) != null) {
                    int parent_idx = filled.getIndexFromName(rep.parent.name);
                    rep.parent = filled.get(parent_idx);
                    filled.add(rep);
                }
            }
        }

        return filled;
    }

    /*
    public static void printBeforeNodes(ObjectRepList filled) {
        int i = 0;
        for (ObjectRep test : filled) {
            if (i > 2) {  // ignore Object, String, Class
                System.out.println(test.name);
                System.out.println("printing class layout . . .");
                System.out.println("=======================");
                ArrayList<Field> fields = test.classRep.fields;
                System.out.println("printing class-fields . . . ");
                for (Field field : fields) {
                    System.out.println("********************");
                    System.out.println(field.access_modifier);
                    System.out.println(field.is_static);
                    System.out.println(field.field_type);
                    System.out.println(field.field_name);
                    System.out.println(field.initial);
                    System.out.println("********************");
                }
                ArrayList<Constructor> constructors = test.classRep.constructors;
                System.out.println("printing class-contructor declarations . . . ");
                for (Constructor constructor : constructors) {
                    System.out.println("********************");
                    System.out.println(constructor.access_modifier);
                    System.out.println(constructor.constructor_name);
                    ArrayList<Parameter> params = constructor.parameters;
                    System.out.println("printing associated parameters . . . ");
                    for (Parameter param : params) {
                        System.out.println("--------------------");
                        System.out.println(param.type);
                        System.out.println(param.name);
                        System.out.println("--------------------");
                    }
                    System.out.println("********************");
                }
                ArrayList<Method> methods = test.classRep.methods;
                System.out.println("printing class-method declarations . . . ");
                for (Method method : methods) {
                    System.out.println("********************");
                    System.out.println(method.access_modifier);
                    System.out.println(method.is_static);
                    System.out.println(method.return_type);
                    System.out.println(method.method_name);
                    ArrayList<Parameter> params = method.parameters;
                    System.out.println("printing associated parameters . . . ");
                    for (Parameter param : params) {
                        System.out.println("--------------------");
                        System.out.println(param.type);
                        System.out.println(param.name);
                        System.out.println("--------------------");
                    }
                    System.out.println("********************");
                }
                System.out.println("=======================");
                System.out.println("printing v table layout . . .");
                System.out.println("=======================");
                ArrayList<Field> vfields = test.vtable.fields;
                System.out.println("printing v-fields . . . ");
                for (Field vfield : vfields) {
                    System.out.println("********************");
                    System.out.println(vfield.access_modifier);
                    System.out.println(vfield.is_static);
                    System.out.println(vfield.field_type);
                    System.out.println(vfield.field_name);
                    System.out.println(vfield.initial);
                    System.out.println("********************");
                }
                ArrayList<VMethod> vmethods = test.vtable.methods;
                System.out.println("printing v-method declarations . . . ");
                for (VMethod vmethod : vmethods) {
                    System.out.println("********************");
                    System.out.println(vmethod.access_modifier);
                    System.out.println(vmethod.is_static);
                    System.out.println(vmethod.method_name);
                    System.out.println(vmethod.initial);
                    System.out.println("********************");
                }
                System.out.println("=======================");
            }
            i++;
        }
    }
    */

    /**
     * Helper method for getFilledObjectRepList
     * @param current
     * @param parent
     * @return ObjectRep of Field
     */
    public static ObjectRep processFields(ObjectRep current, ObjectRep parent) {
        // process fields according to the following rules:
        // private instance fields are inherited (there is a workaround way of accessing them)
        // static instance fields are not inherited (static leads to initialization issues)
        // preserve order too
        ObjectRep currentPrime = determineFields(current, parent);

        return currentPrime;
    }

    /**
     * Determines the field variables
     * @param current
     * @param parent
     * @return current ObjectRep with update fields
     */
    public static ObjectRep determineFields(ObjectRep current, ObjectRep parent) {
        // iterate over parent fields from class, if possible to inherit, add to child, then dump child methods, this way we preserve order
        ArrayList<Field> parentFields = parent.classRep.fields;
        ArrayList<Field> currentFields = current.classRep.fields;

        // new array list to dump fields into as they are processed
        ArrayList<Field> updatedFields = new ArrayList<Field>();
        HashSet<String> updatedFieldNames = new HashSet<String>();

        // process parent fields and inherit valid fields
        for (Field parentField : parentFields) {
            // if field is static it can't be inherited, furthermore don't inherit the vptr and the vtable
            if (parentField.isStatic == false && !parentField.fieldName.equals("__vptr") && !parentField.fieldName.equals("__vtable")) {
                updatedFields.add(parentField);
                updatedFieldNames.add(parentField.fieldName);
            }
        }

        // process current fields and dump fields to updated fields, if same name is used we assume no shadowed fields therefore no reason to re-declare
        for (Field currentField : currentFields) {
            if (!updatedFieldNames.contains(currentField.fieldName)) {
                updatedFields.add(currentField);
                updatedFieldNames.add(currentField.fieldName);
            }
        }

        current.classRep.fields = updatedFields;

        return current;
    }

    /**
     * Helper method for getFilledObjectRepList
     * @param current
     * @param parent
     * @return ObjectRep for VTable
     */
    public static ObjectRep processVTable(ObjectRep current, ObjectRep parent) {

        // first process the methods of the class declaration
        // note: constructor processing is not required at the moment
        // fields are simply expanded out in visitor with corresponding method definitions
        // similarly update methods using relationship between an object and its parent
        // constructors are basically simple, multiple constructors currently not allowed
        ObjectRep currentPrime = determineVTable(current, parent);

        return currentPrime;
    }

    /**
     * Determines VTable for given ObjectRep
     * @param current
     * @param parent
     * @return ObjectRep with updated VTable and its fields
     */
    public static ObjectRep determineVTable(ObjectRep current, ObjectRep parent) {
        ArrayList<Field> parentFields = parent.vtable.fields;
        ArrayList<Method> currentMethods = current.classRep.methods;

        VMethod __is_a = current.vtable.methods.get(0);

        ArrayList<Field> updatedFields = new ArrayList<Field>();
        ArrayList<VMethod> updatedVMethods = new ArrayList<VMethod>();

        // determine method declarations dependent on parent declarations (overwritten or not)
        for (Field parentField : parentFields) {
            // boolean to check if something was updated
            boolean notUpdated = true;
            // loop over current methods to determine what has been overwritten and what hasn't, this will ensure preservation of order too
            for (Method currentMethod : currentMethods) {
                // if method is overwritten by child, need extra processing, ignore class definition
                if (parentField.fieldName.replaceFirst("\\*","").equals(currentMethod.name) && parentField.isStatic == false && !parentField.accessModifier.equals("private")) {
                    // process parameters correctly into field declaration
                    String parameters = "";
                    parameters += current.name;
                    ArrayList<Parameter> params = currentMethod.parameters;
                    for (Parameter param : params) parameters += "," + param.type;
                    Field temp = new Field(currentMethod.accessModifier, false, currentMethod.returnType, "*"+currentMethod.name, parameters);
                    temp.inheritedFrom = current.name;
                    updatedFields.add(temp);
                    notUpdated = false;
                    // process parameters correctly into a vMethod declaration
                    updatedVMethods.add(new VMethod(currentMethod.accessModifier, false, currentMethod.name, "(&__"+current.name+"::"+currentMethod.name+")"));
                }
            }
            // if method wasn't overwritten and is not class (which is initialized in ObjectRep creation), modify its args and simply add to updated_fields list, also add inheritnce to updated vMethods list (these will refer to Object)
            if (notUpdated && parentField.isStatic == false && !parentField.accessModifier.equals("private")) {
                String inheritedFrom = "";
                if (parentField.inheritedFrom.equals("")) inheritedFrom = "Object";
                else inheritedFrom = parentField.inheritedFrom;
                Field temp = new Field(parentField.accessModifier, parentField.isStatic, parentField.fieldType, parentField.fieldName, parentField.initial.replaceFirst(parent.name, current.name));
                temp.inheritedFrom = inheritedFrom;
                updatedFields.add(temp);
                if (parentField.fieldName.equals("__is_a")) updatedVMethods.add(__is_a);
                else updatedVMethods.add(new VMethod(parentField.accessModifier, parentField.isStatic, parentField.fieldName.replaceFirst("\\*",""), "(("+parentField.fieldType+"(*)("+parentField.initial.replaceFirst(parent.name, current.name)+")) &__"+inheritedFrom+"::"+parentField.fieldName.replaceFirst("\\*","")+")"));
            }
        }

        ArrayList<Field> updatedFieldsPrime = new ArrayList<Field>();
        ArrayList<VMethod> updatedVMethodsPrime = new ArrayList<VMethod>();

        // use hashset of names for uniqueness property
        HashSet<String> updatedFieldSet = new HashSet<String>();
        for (Field updatedField : updatedFields) updatedFieldSet.add(updatedField.fieldName.replaceFirst("\\*",""));

        // dump rest of methods in current_methods into updated_fields and set precedent for order + preserve order
        for (Method currentMethod : currentMethods) {
            // if not already declared, declare it now, also ignore private methods since they do not get vtable entries, also ignore static methods since they do not get vtable entries too
            if (!updatedFieldSet.contains(currentMethod.name) && !currentMethod.accessModifier.equals("private") && currentMethod.isStatic == false) {
                // process parameters correctly into field declaration
                String parameters = "";
                parameters += current.name;
                ArrayList<Parameter> params = currentMethod.parameters;
                for (Parameter param : params) parameters +=  "," + param.type;
                Field temp = new Field(currentMethod.accessModifier, false, currentMethod.returnType, "*"+currentMethod.name, parameters);
                temp.inheritedFrom = current.name;
                updatedFieldsPrime.add(temp);
                // ("+currentMethod.returnType+"(*)("+parameters+")) this was removed from init, keeping it here just in case
                updatedVMethodsPrime.add(new VMethod(currentMethod.accessModifier, false, currentMethod.name, "(&__"+current.name+"::"+currentMethod.name+")"));
            }
        }

        for (Field field : updatedFieldsPrime) updatedFields.add(field);
        for (VMethod vmethod : updatedVMethodsPrime) updatedVMethods.add(vmethod);

        current.vtable.fields = updatedFields;
        current.vtable.methods = updatedVMethods;

        return current;
    }

    /**
     * Initializes ObjectReps for Object, String and Class layout and structures
     * @return ObjectRepList with Object, String and Class
     */
    public static ObjectRepList initializeRepList() {

        ObjectRepList filled = new ObjectRepList();

        // Adding java.lang structure manually for Object
        ObjectRep objectRep = new ObjectRep("Object");
        ArrayList<Parameter> params = new ArrayList<Parameter>();
        params.add(new Parameter("Object", "o"));
        Method hashCode = new Method("public", true, "int32_t", "hashCode", params);
        objectRep.classRep.methods.add(hashCode);
        params.add(new Parameter("Object", "o"));
        Method equals = new Method("public", true, "bool", "equals", params);
        objectRep.classRep.methods.add(equals);
        params.remove(1);
        Method getClass = new Method("public", true, "Class", "getClass", params);
        objectRep.classRep.methods.add(getClass);
        Method toString = new Method("public", true, "String", "toString", params);
        objectRep.classRep.methods.add(toString);
        // class representation for Object filled, now do V-Table filling
        Field v_hashCode = new Field("public", false, "int32_t", "*hashCode", "Object");
        objectRep.vtable.fields.add(v_hashCode);
        Field v_equals = new Field("public", false, "bool", "*equals", "Object, Object");
        objectRep.vtable.fields.add(v_equals);
        Field v_getClass = new Field("public", false, "Class", "*getClass", "Object");
        objectRep.vtable.fields.add(v_getClass);
        Field v_toString = new Field("public", false, "String", "*toString", "Object");
        objectRep.vtable.fields.add(v_toString);
        VMethod v_method_hashCode = new VMethod("public", false, "hashCode", "(&__Object::__hashCode)");
        objectRep.vtable.methods.add(v_method_hashCode);
        VMethod v_method_equals = new VMethod("public", false, "equals", "(&__Object::__equals)");
        objectRep.vtable.methods.add(v_method_equals);
        VMethod v_method_getClass = new VMethod("public", false, "getClass", "(&__Object::__getClass)");
        objectRep.vtable.methods.add(v_method_getClass);
        VMethod v_method_toString = new VMethod("public", false, "toString", "(&__Object::__toString)");
        objectRep.vtable.methods.add(v_method_toString);
        // set parent to null
        objectRep.parent = null;
        // object is created!
        filled.add(objectRep);

        // Adding java.lang structure manually for String
        ObjectRep stringRep = new ObjectRep("String");
        Field data = new Field("public", false, "std::string", "data", "");
        stringRep.classRep.fields.add(data);
        params = new ArrayList<Parameter>();
        params.add(new Parameter("std::string", "data"));
        Constructor stringConstructor = new Constructor("public", "__String", params);
        stringRep.classRep.constructors.remove(0);
        stringRep.classRep.constructors.add(stringConstructor);
        params = new ArrayList<Parameter>();
        params.add(new Parameter("String", "str"));
        hashCode = new Method("public", true, "int32_t", "hashCode", params);
        stringRep.classRep.methods.add(hashCode);
        params.add(new Parameter("Object", "o"));
        equals = new Method("public", true, "bool", "equals", params);
        stringRep.classRep.methods.add(equals);
        params.remove(1);
        toString = new Method("public", true, "int32_t", "length", params);
        stringRep.classRep.methods.add(toString);
        params.add(new Parameter("int32_t", "int_length"));
        Method charAt = new Method("public", true, "char", "charAt", params);
        stringRep.classRep.methods.add(charAt);
        // class representation for String filled, now do V-Table filling
        v_hashCode = new Field("public", false, "int32_t", "*hashCode", "String");
        stringRep.vtable.fields.add(v_hashCode);
        v_equals = new Field("public", false, "bool", "*equals", "String, Object");
        stringRep.vtable.fields.add(v_equals);
        v_getClass = new Field("public", false, "Class", "*getClass", "String");
        stringRep.vtable.fields.add(v_getClass);
        v_toString = new Field("public", false, "String", "*toString", "String");
        stringRep.vtable.fields.add(v_toString);
        Field v_length = new Field("public", false, "int32_t", "*length", "String");
        stringRep.vtable.fields.add(v_length);
        Field v_charAt = new Field("public", false, "char", "*charAt", "String");
        stringRep.vtable.fields.add(v_charAt);
        v_method_hashCode = new VMethod("public", false, "hashCode", "(&__String::__hashCode())");
        stringRep.vtable.methods.add(v_method_hashCode);
        v_method_equals = new VMethod("public", false, "equals", "(&__String::equals)");
        stringRep.vtable.methods.add(v_method_equals);
        v_method_getClass = new VMethod("public", false, "getClass", "((Class(*)(String)) &__Object::getClass");
        stringRep.vtable.methods.add(v_method_getClass);
        v_method_toString = new VMethod("public", false, "toString", "(&__toString::toString)");
        stringRep.vtable.methods.add(v_method_toString);
        VMethod v_method_charAt = new VMethod("public", false, "charAt", "(&__String::charAt)");
        stringRep.vtable.methods.add(v_method_charAt);
        // set parent to Object
        stringRep.parent = objectRep;
        // String is created!
        filled.add(stringRep);

        // Adding java.lang structure manually for Class
        ObjectRep classRep = new ObjectRep("Class");
        Field name = new Field("public", false, "String", "name", "");
        classRep.classRep.fields.add(name);
        Field parent = new Field("public", false, "Class", "parent", "");
        classRep.classRep.fields.add(parent);
        params = new ArrayList<Parameter>();
        params.add(new Parameter("String", "name"));
        params.add(new Parameter("Class", "parent"));
        Constructor classConstructor = new Constructor("public", "__Class", params);
        classRep.classRep.constructors.remove(0);
        classRep.classRep.constructors.add(classConstructor);
        params = new ArrayList<Parameter>();
        params.add(new Parameter("Class", "c"));
        toString = new Method("public", true, "String", "toString", params);
        classRep.classRep.methods.add(toString);
        Method getName = new Method("public", true, "String", "getName", params);
        classRep.classRep.methods.add(getName);
        Method getSuperClass = new Method("public", true, "Class", "getSuperclass", params);
        classRep.classRep.methods.add(getSuperClass);
        params.add(new Parameter("Object", "o"));
        Method isInstance = new Method("public", true, "bool", "isInstance", params);
        classRep.classRep.methods.add(isInstance);
        // class representation for Class filled, now do V-Table filling
        v_hashCode = new Field("public", false, "int32_t", "*hashCode", "Class");
        classRep.vtable.fields.add(v_hashCode);
        v_equals = new Field("public", false, "bool", "*equals", "Class, Object");
        classRep.vtable.fields.add(v_equals);
        v_getClass = new Field("public", false, "Class", "*getClass", "Class");
        classRep.vtable.fields.add(v_getClass);
        v_toString = new Field("public", false, "String", "*toString", "Class");
        classRep.vtable.fields.add(v_toString);
        Field v_getName = new Field("public", false, "String", "*getName", "Class");
        classRep.vtable.fields.add(v_getName);
        Field v_getSuperClass = new Field("public", false, "String", "*getSuperclass", "Class");
        classRep.vtable.fields.add(v_getSuperClass);
        Field v_isInstance = new Field("public", false, "String", "*isInstance", "Class");
        classRep.vtable.fields.add(v_isInstance);
        v_method_hashCode = new VMethod("public", false, "hashCode", "((int32_t(*)(Class)) &__Object::hashCode)");
        classRep.vtable.methods.add(v_method_hashCode);
        v_method_equals = new VMethod("public", false, "equals", "((bool(*)(Class,Object)) &__Object::equals)");
        classRep.vtable.methods.add(v_method_equals);
        v_method_getClass = new VMethod("public", false, "getClass", "((Class(*)(Class)) &__Object::getClass)");
        classRep.vtable.methods.add(v_method_getClass);
        v_method_toString = new VMethod("public", false, "toString", "(&__Class::toString)");
        classRep.vtable.methods.add(v_method_toString);
        VMethod v_method_getName = new VMethod("public", false, "getName", "(&__Class::getName)");
        classRep.vtable.methods.add(v_method_getName);
        VMethod v_method_getSuperClass = new VMethod("public", false, "getSuperclass", "(&__Class::getSuperclass)");
        classRep.vtable.methods.add(v_method_getSuperClass);
        VMethod v_method_isInstance = new VMethod("public", false, "isInstance", "(&__Class::isInstance)");
        classRep.vtable.methods.add(v_method_isInstance);
        // set parent to object
        classRep.parent = objectRep;
        // class is created!
        filled.add(classRep);

        return filled;
    }

    /**
     * Method to create root node and attach ObjectReps to it giving the final AST
     * @param packageName
     * @param ObjectRepList
     * @return cppAst
     */
    public static Node buildCppAst(String packageName, ObjectRepList ObjectRepList) {
        // root
        Node root = GNode.create("CompilationUnit");

        // package
        Node packageDeclaration = GNode.create("PackageDeclaration", packageName);
        root.add(packageDeclaration);

        // forward declarations
        Node forwardDeclarations = GNode.create("ForwardDeclarations");
        root.add(forwardDeclarations);

        // process each object representation
        for (ObjectRep rep : ObjectRepList) {
            // add to forward declarations
            forwardDeclarations.add(rep.name);
            // add class node
            root.add(buildClassNode(rep));
        }

        return root;
    }

    /**
     * Helper method for buildCppAst
     * @param rep
     * @return Class node with layout and Vtable
     */
    public static Node buildClassNode(ObjectRep rep) {
        // name, commented out not in sai's code but here just in case
        // Node name = GNode.create("ClassName", rep.name);

        // fields
        Node fields = GNode.create("FieldDeclarations");
        for(Field field : rep.classRep.fields) fields.add(buildFieldNode(field));

        // constructors
        Node constructors = GNode.create("ConstructorDeclarations");
        for(Constructor constructor : rep.classRep.constructors) constructors.add(buildConstructorNode(constructor));

        // methods
        Node methods = GNode.create("MethodDeclarations");
        for(Method method : rep.classRep.methods) methods.add(buildMethodNode(method));

        // vtable and declaration
        Node vFieldDeclaration = GNode.create("VFieldDec");
        Node vTable = buildVTableNode(rep.name, rep.vtable.fields, rep.vtable.methods);

        // data layout node
        Node dataLayout = GNode.create("DataLayout",fields, constructors, methods, vFieldDeclaration);

        // return class declaration
        return GNode.create("ClassDeclaration", rep.name, dataLayout, vTable);
    }

    /**
     * Helper method for buildClassNode
     * @param field
     * @return node with Fields Ast
     */
    public static Node buildFieldNode(Field field) {
        Node isStatic = GNode.create("IsStatic", String.valueOf(field.isStatic));
        Node fieldType = GNode.create("FieldType", field.fieldType);
        Node fieldName = GNode.create("FieldName", field.fieldName);
        Node initial = GNode.create("Initial", field.initial);
        return GNode.create("Field", isStatic, fieldType, fieldName, initial);
    }

    /**
     * Helper methods for buidClassNode
     * @param constructor
     * @return node with constructor Ast
     */
    public static Node buildConstructorNode(Constructor constructor) {
        Node constructorName = GNode.create("ConstructorName", constructor.name);
        Node constructorParameters = GNode.create("ConstructorParameters");
        for(Parameter parameter : constructor.parameters) {
            Node parameterType = GNode.create("ParameterType", parameter.type);
            Node parameterName = GNode.create("ParameterName", parameter.name);
            Node parameterNode = GNode.create("Parameter", parameterType, parameterName);
            constructorParameters.add(parameterNode);
        }

        return GNode.create("ConstructorDeclaration", constructorName, constructorParameters);
    }

    /**
     * Helper method for buildClassAst
     * @param method
     * @return method node Ast
     */
    public static Node buildMethodNode(Method method) {
        Node isStatic = GNode.create("IsStatic", String.valueOf(method.isStatic));
        Node returnType = GNode.create("ReturnType", method.returnType);
        Node methodName = GNode.create("MethodName", method.name);
        Node methodParameters = GNode.create("MethodParameters");
        for(Parameter parameter : method.parameters) {
            Node parameterType = GNode.create("ParameterType", parameter.type);
            Node parameterName = GNode.create("ParameterName", parameter.name);
            Node parameterNode = GNode.create("Parameter", parameterType, parameterName);
            methodParameters.add(parameterNode);
        }

        return GNode.create("MethodDeclaration", isStatic, returnType, methodName, methodParameters);
    }

    /**
     * Helper method for buildClassAst
     * @param name
     * @param vfields
     * @param vmethods
     * @return node vtable Ast
     */
    public static Node buildVTableNode(String name, ArrayList<Field> vfields, ArrayList<VMethod> vmethods) {
        // root
        Node root = GNode.create("VTableLayout");
        // add name to root
        root.add(name);
        // fields
        Node fields = GNode.create("VFields");
        // process vfields
        for (Field vfield : vfields) {
            Node fieldType = GNode.create("FieldType", vfield.fieldType);
            Node fieldName = GNode.create("FieldName", vfield.fieldName);
            Node initial = GNode.create("Initial", vfield.initial);
            fields.add(GNode.create("VField", fieldType, fieldName, initial));
        }
        root.add(fields);
        // methods
        Node methods = GNode.create("VMethods");
        // process vmethods
        for(VMethod vmethod : vmethods) {
            Node methodName = GNode.create("MethodName", vmethod.name);
            Node initial = GNode.create("Initial", vmethod.initial);
            methods.add(GNode.create("VMethod", methodName, initial));
        }
        root.add(methods);

        return root;
    }
}