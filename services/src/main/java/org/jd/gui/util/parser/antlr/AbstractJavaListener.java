/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.gui.util.parser.antlr;

import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.antlr.v4.runtime.tree.TerminalNodeImpl;
import org.jd.gui.api.model.Container;
import org.jd.gui.util.exception.ExceptionUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class AbstractJavaListener extends JavaParserBaseListener {
    protected Container.Entry entry;
    protected String packageName = "";
    protected HashMap<String, String> nameToInternalTypeName = new HashMap<>();
    protected StringBuilder sb = new StringBuilder();
    protected HashMap<String, String> typeNameCache = new HashMap<>();

    public AbstractJavaListener(Container.Entry entry) {
        this.entry = entry;
    }

    public void enterPackageDeclaration(JavaParser.PackageDeclarationContext ctx) {
        packageName = concatIdentifiers(identifiers(ctx.qualifiedName()));
    }

    public void enterImportDeclaration(JavaParser.ImportDeclarationContext ctx) {
        List<TerminalNode> identifiers = identifiers(ctx.qualifiedName());
        int size = identifiers.size();

        if (size > 1) {
            nameToInternalTypeName.put(identifiers.get(size - 1).getText(), concatIdentifiers(identifiers));
        }
    }

    // --- Grammar helpers --- //

    /** @return the token of an 'identifier' or 'typeIdentifier' rule, or null */
    protected static TerminalNode identifier(ParserRuleContext ctx) {
        return (ctx == null) ? null : ctx.getChild(TerminalNode.class, 0);
    }

    protected static List<TerminalNode> identifiers(List<? extends ParserRuleContext> contexts) {
        List<TerminalNode> identifiers = new ArrayList<>(contexts.size());

        for (ParserRuleContext context : contexts) {
            identifiers.add(identifier(context));
        }

        return identifiers;
    }

    protected static List<TerminalNode> identifiers(JavaParser.QualifiedNameContext ctx) {
        return identifiers(ctx.identifier());
    }

    /** @return the name token of a type declaration (class, enum, interface, annotation, record) */
    protected static TerminalNode typeDeclarationIdentifier(ParserRuleContext ctx) {
        return identifier(ctx.getRuleContext(JavaParser.IdentifierContext.class, 0));
    }

    /**
     * @return the identifiers of a class or interface type name, without type arguments and annotations
     *         ("java.util.Map<K, V>.Entry" -> [java, util, Map, Entry])
     */
    protected static List<TerminalNode> identifiers(JavaParser.ClassOrInterfaceTypeContext ctx) {
        List<TerminalNode> identifiers = new ArrayList<>();
        collectTypeNameIdentifiers(ctx, identifiers);
        return identifiers;
    }

    private static void collectTypeNameIdentifiers(ParserRuleContext ctx, List<TerminalNode> identifiers) {
        if (ctx.children != null) {
            for (ParseTree child : ctx.children) {
                if ((child instanceof JavaParser.IdentifierContext) || (child instanceof JavaParser.TypeIdentifierContext)) {
                    identifiers.add(identifier((ParserRuleContext)child));
                } else if ((child instanceof ParserRuleContext) &&
                        !(child instanceof JavaParser.TypeArgumentsContext) && !(child instanceof JavaParser.AnnotationContext)) {
                    collectTypeNameIdentifiers((ParserRuleContext)child, identifiers);
                }
            }
        }
    }

    /**
     * Formal parameters, except receiver parameters.
     *
     * @return null if there is no parameter list
     */
    protected static List<JavaParser.FormalParameterContext> formalParameters(JavaParser.FormalParametersContext ctx) {
        List<JavaParser.FormalParameterContext> formalParameters = new ArrayList<>();
        boolean empty = true;

        if (ctx.children != null) {
            for (ParseTree child : ctx.children) {
                if (child instanceof JavaParser.FormalParameterContext) {
                    empty = false;
                    formalParameters.add((JavaParser.FormalParameterContext)child);
                } else if (child instanceof JavaParser.FormalParameterListContext) {
                    empty = false;
                    formalParameters.addAll(((JavaParser.FormalParameterListContext)child).formalParameter());
                }
            }
        }

        return empty ? null : formalParameters;
    }

    /**
     * Body of an anonymous class: "new T(...) { ... }" or an enum constant with a body.
     */
    protected static boolean isAnonymousClassBody(JavaParser.ClassBodyContext ctx) {
        ParserRuleContext parent = ctx.getParent();
        return (parent instanceof JavaParser.ClassCreatorRestContext) || (parent instanceof JavaParser.EnumConstantContext);
    }

    /**
     * Dimension declared after the parameter name, plus one for a variable arity parameter.
     */
    protected int countDimension(JavaParser.FormalParameterContext formalParameter) {
        int dimension = countDimension(formalParameter.variableDeclaratorId().children);
        return (formalParameter.ELLIPSIS() == null) ? dimension : dimension + 1;
    }

    protected String concatIdentifiers(List<TerminalNode> identifiers) {
        switch (identifiers.size()) {
            case 0:
                return "";
            case 1:
                return identifiers.get(0).getText();
            default:
                sb.setLength(0);

                for (TerminalNode identifier : identifiers) {
                    sb.append(identifier.getText()).append('/');
                }

                // Remove last separator
                sb.setLength(sb.length() - 1);

                return sb.toString();
        }
    }

    protected String resolveInternalTypeName(List<TerminalNode> identifiers) {
        switch (identifiers.size()) {
            case 0:
                return null;

            case 1:
                // Search in cache
                String name = identifiers.get(0).getText();
                String qualifiedName = typeNameCache.get(name);

                if (qualifiedName != null) {
                    return qualifiedName;
                }

                // Search in imports
                String imp = nameToInternalTypeName.get(name);

                if (imp != null) {
                    // Import found
                    return imp;
                }

                // Search type in same package
                String prefix = name + '.';

                if (entry.getPath().indexOf('/') != -1) {
                    // Not in root package
                    Container.Entry parent = entry.getParent();
                    int packageLength = parent.getPath().length() + 1;

                    for (Container.Entry child : parent.getChildren()) {
                        if (!child.isDirectory() && child.getPath().substring(packageLength).startsWith(prefix)) {
                            qualifiedName = packageName + '/' + name;
                            typeNameCache.put(name, qualifiedName);
                            return qualifiedName;
                        }
                    }
                }

                // Search type in root package
                for (Container.Entry child : entry.getContainer().getRoot().getChildren()) {
                    if (!child.isDirectory() && child.getPath().startsWith(prefix)) {
                        typeNameCache.put(name, name);
                        return name;
                    }
                }

                // Search type in 'java.lang'
                try {
                    if (Class.forName("java.lang." + name) != null) {
                        qualifiedName = "java/lang/" + name;
                        typeNameCache.put(name, qualifiedName);
                        return qualifiedName;
                    }
                } catch (ClassNotFoundException ignore) {
                    // Ignore class loading error
                }

                // Type not found
                qualifiedName = "*/" + name;
                typeNameCache.put(name, qualifiedName);
                return qualifiedName;

            default:
                // Qualified type name -> Nothing to do
                return concatIdentifiers(identifiers);
        }
    }

    protected String createDescriptor(JavaParser.TypeTypeOrVoidContext typeContext, int dimension) {
        return createDescriptor((typeContext == null) ? null : typeContext.typeType(), dimension);
    }

    protected String createDescriptor(JavaParser.TypeTypeContext typeContext, int dimension) {
        if (typeContext == null) {
            return "V";
        } else {
            dimension += countDimension(typeContext.children);
            JavaParser.PrimitiveTypeContext primitive = typeContext.primitiveType();
            String name;

            if (primitive == null) {
                name = "L" + resolveInternalTypeName(identifiers(typeContext.classOrInterfaceType())) + ";";
            } else {
                // Search primitive
                switch (primitive.getText()) {
                    case "boolean": name = "Z"; break;
                    case "byte":    name = "B"; break;
                    case "char":    name = "C"; break;
                    case "double":  name = "D"; break;
                    case "float":   name = "F"; break;
                    case "int":     name = "I"; break;
                    case "long":    name = "J"; break;
                    case "short":   name = "S"; break;
                    case "void":    name = "V"; break;
                    default:
                        throw new RuntimeException("UNEXPECTED PRIMITIVE");
                }
            }

            switch (dimension) {
                case 0:  return name;
                case 1:  return "[" + name;
                case 2:  return "[[" + name;
                default: return new String(new char[dimension]).replace('\0', '[') + name;
            }
        }
    }

    protected int countDimension(List<ParseTree> children) {
        int dimension = 0;

        for (ParseTree child : children) {
            if (child instanceof TerminalNodeImpl) {
                if (((TerminalNodeImpl)child).getSymbol().getType() == JavaParser.LBRACK)
                    dimension++;
            }
        }

        return dimension;
    }
}
