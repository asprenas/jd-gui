/*
 * Copyright (c) 2008-2019 Emmanuel Dupuy.
 * This project is distributed under the GPLv3 license.
 * This is a Copyleft license that gives the user the right to use,
 * copy and modify the code freely for non-commercial purposes.
 */

package org.jd.gui.service.indexer;

import org.antlr.v4.runtime.CharStreams;
import org.antlr.v4.runtime.ParserRuleContext;
import org.antlr.v4.runtime.tree.ParseTree;
import org.antlr.v4.runtime.tree.TerminalNode;
import org.jd.gui.api.API;
import org.jd.gui.api.model.Container;
import org.jd.gui.api.model.Indexes;
import org.jd.gui.util.exception.ExceptionUtil;
import org.jd.gui.util.parser.antlr.ANTLRJavaParser;
import org.jd.gui.util.parser.antlr.AbstractJavaListener;
import org.jd.gui.util.parser.antlr.JavaParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

/**
 * Unsafe thread implementation of java file indexer.
 */
public class JavaFileIndexerProvider extends AbstractIndexerProvider {

    static {
        // Early class loading
        ANTLRJavaParser.parse(CharStreams.fromString("class EarlyLoading{}"), new Listener(null));
    }

    @Override public String[] getSelectors() { return appendSelectors("*:file:*.java"); }

    @Override
    @SuppressWarnings("unchecked")
    public void index(API api, Container.Entry entry, Indexes indexes) {
        try (InputStream inputStream = entry.getInputStream()) {
            Listener listener = new Listener(entry);
            ANTLRJavaParser.parse(CharStreams.fromStream(inputStream), listener);

            // Append sets to indexes
            addToIndexes(indexes, "typeDeclarations", listener.getTypeDeclarationSet(), entry);
            addToIndexes(indexes, "constructorDeclarations", listener.getConstructorDeclarationSet(), entry);
            addToIndexes(indexes, "methodDeclarations", listener.getMethodDeclarationSet(), entry);
            addToIndexes(indexes, "fieldDeclarations", listener.getFieldDeclarationSet(), entry);
            addToIndexes(indexes, "typeReferences", listener.getTypeReferenceSet(), entry);
            addToIndexes(indexes, "constructorReferences", listener.getConstructorReferenceSet(), entry);
            addToIndexes(indexes, "methodReferences", listener.getMethodReferenceSet(), entry);
            addToIndexes(indexes, "fieldReferences", listener.getFieldReferenceSet(), entry);
            addToIndexes(indexes, "strings", listener.getStringSet(), entry);

            // Populate map [super type name : [sub type name]]
            Map<String, Collection> index = indexes.getIndex("subTypeNames");

            for (Map.Entry<String, HashSet<String>> e : listener.getSuperTypeNamesMap().entrySet()) {
                String typeName = e.getKey();

                for (String superTypeName : e.getValue()) {
                    index.get(superTypeName).add(typeName);
                }
            }
        } catch (IOException e) {
            assert ExceptionUtil.printStackTrace(e);
        }
    }

    protected static class Listener extends AbstractJavaListener {

        protected HashSet<String> typeDeclarationSet = new HashSet<>();
        protected HashSet<String> constructorDeclarationSet = new HashSet<>();
        protected HashSet<String> methodDeclarationSet = new HashSet<>();
        protected HashSet<String> fieldDeclarationSet = new HashSet<>();
        protected HashSet<String> typeReferenceSet = new HashSet<>();
        protected HashSet<String> constructorReferenceSet = new HashSet<>();
        protected HashSet<String> methodReferenceSet = new HashSet<>();
        protected HashSet<String> fieldReferenceSet = new HashSet<>();
        protected HashSet<String> stringSet = new HashSet<>();
        protected HashMap<String, HashSet<String>> superTypeNamesMap = new HashMap<>();

        protected StringBuilder sbTypeDeclaration = new StringBuilder();

        public Listener(Container.Entry entry) {
            super(entry);
        }

        public HashSet<String> getTypeDeclarationSet() { return typeDeclarationSet; }
        public HashSet<String> getConstructorDeclarationSet() { return constructorDeclarationSet; }
        public HashSet<String> getMethodDeclarationSet() { return methodDeclarationSet; }
        public HashSet<String> getFieldDeclarationSet() { return fieldDeclarationSet; }
        public HashSet<String> getTypeReferenceSet() { return typeReferenceSet; }
        public HashSet<String> getConstructorReferenceSet() { return constructorReferenceSet; }
        public HashSet<String> getMethodReferenceSet() { return methodReferenceSet; }
        public HashSet<String> getFieldReferenceSet() { return fieldReferenceSet; }
        public HashSet<String> getStringSet() { return stringSet; }
        public HashMap<String, HashSet<String>> getSuperTypeNamesMap() { return superTypeNamesMap; }

        // --- ANTLR Listener --- //

        public void enterPackageDeclaration(JavaParser.PackageDeclarationContext ctx) {
            super.enterPackageDeclaration(ctx);

            if (! packageName.isEmpty()) {
                sbTypeDeclaration.append(packageName).append('/');
            }
        }

        public void enterClassDeclaration(JavaParser.ClassDeclarationContext ctx) { enterTypeDeclaration(ctx); }
        public void exitClassDeclaration(JavaParser.ClassDeclarationContext ctx) { exitTypeDeclaration(); }

        public void enterEnumDeclaration(JavaParser.EnumDeclarationContext ctx) { enterTypeDeclaration(ctx); }
        public void exitEnumDeclaration(JavaParser.EnumDeclarationContext ctx) { exitTypeDeclaration(); }

        public void enterInterfaceDeclaration(JavaParser.InterfaceDeclarationContext ctx) { enterTypeDeclaration(ctx); }
        public void exitInterfaceDeclaration(JavaParser.InterfaceDeclarationContext ctx) { exitTypeDeclaration(); }

        public void enterAnnotationTypeDeclaration(JavaParser.AnnotationTypeDeclarationContext ctx) { enterTypeDeclaration(ctx); }
        public void exitAnnotationTypeDeclaration(JavaParser.AnnotationTypeDeclarationContext ctx) { exitTypeDeclaration(); }

        protected void enterTypeDeclaration(ParserRuleContext ctx) {
            // Add type declaration
            TerminalNode identifier = typeDeclarationIdentifier(ctx);

            if (identifier != null) {
                String typeName = identifier.getText();
                int length = sbTypeDeclaration.length();

                if ((length == 0) || (sbTypeDeclaration.charAt(length - 1) == '/')) {
                    sbTypeDeclaration.append(typeName);
                } else {
                    sbTypeDeclaration.append('$').append(typeName);
                }

                String internalTypeName = sbTypeDeclaration.toString();
                typeDeclarationSet.add(internalTypeName);
                nameToInternalTypeName.put(typeName, internalTypeName);

                HashSet<String> superInternalTypeNameSet = new HashSet<>();

                // Add super type reference
                JavaParser.TypeTypeContext superType = ctx.getRuleContext(JavaParser.TypeTypeContext.class, 0);
                if ((superType != null) && (superType.classOrInterfaceType() != null)) {
                    String superQualifiedTypeName = resolveInternalTypeName(identifiers(superType.classOrInterfaceType()));

                    if (superQualifiedTypeName.charAt(0) != '*')
                        superInternalTypeNameSet.add(superQualifiedTypeName);
                }

                // Add implementation references
                JavaParser.TypeListContext superInterfaces = ctx.getRuleContext(JavaParser.TypeListContext.class, 0);
                if (superInterfaces != null) {
                    for (JavaParser.TypeTypeContext superInterface : superInterfaces.typeType()) {
                        if (superInterface.classOrInterfaceType() == null)
                            continue;

                        String superQualifiedInterfaceName = resolveInternalTypeName(identifiers(superInterface.classOrInterfaceType()));

                        if (superQualifiedInterfaceName.charAt(0) != '*')
                            superInternalTypeNameSet.add(superQualifiedInterfaceName);
                    }
                }

                if (!superInternalTypeNameSet.isEmpty()) {
                    superTypeNamesMap.put(internalTypeName, superInternalTypeNameSet);
                }
            }
        }

        protected void exitTypeDeclaration() {
            int index = sbTypeDeclaration.lastIndexOf("$");

            if (index == -1) {
                index = sbTypeDeclaration.lastIndexOf("/") + 1;
            }

            if (index == -1) {
                sbTypeDeclaration.setLength(0);
            } else {
                sbTypeDeclaration.setLength(index);
            }
        }

        public void enterTypeType(JavaParser.TypeTypeContext ctx) {
            // Add type reference
            JavaParser.ClassOrInterfaceTypeContext classOrInterfaceType = ctx.classOrInterfaceType();

            if (classOrInterfaceType != null) {
                String internalTypeName = resolveInternalTypeName(identifiers(classOrInterfaceType));

                if (internalTypeName.charAt(0) != '*')
                    typeReferenceSet.add(internalTypeName);
            }
        }

        public void enterConstDeclaration(JavaParser.ConstDeclarationContext ctx) {
            for (JavaParser.ConstantDeclaratorContext constantDeclaratorContext : ctx.constantDeclarator()) {
                String name = identifier(constantDeclaratorContext.identifier()).getText();
                fieldDeclarationSet.add(name);
            }
        }

        public void enterFieldDeclaration(JavaParser.FieldDeclarationContext ctx) {
            for (JavaParser.VariableDeclaratorContext declaration : ctx.variableDeclarators().variableDeclarator()) {
                TerminalNode identifier = identifier(declaration.variableDeclaratorId().identifier());

                if (identifier != null) {
                    String name = identifier.getText();
                    fieldDeclarationSet.add(name);
                }
            }
        }

        public void enterMethodDeclaration(JavaParser.MethodDeclarationContext ctx) {
            TerminalNode identifier = identifier(ctx.identifier());

            if (identifier != null) {
                String name = identifier.getText();
                methodDeclarationSet.add(name);
            }
        }

        public void enterInterfaceCommonBodyDeclaration(JavaParser.InterfaceCommonBodyDeclarationContext ctx) {
            TerminalNode identifier = identifier(ctx.identifier());

            if (identifier != null) {
                String name = identifier.getText();
                methodDeclarationSet.add(name);
            }
        }

        public void enterConstructorDeclaration(JavaParser.ConstructorDeclarationContext ctx) {
            String name = identifier(ctx.identifier()).getText();
            constructorDeclarationSet.add(name);
        }

        public void enterCreatedName(JavaParser.CreatedNameContext ctx) {
            String internalTypeName = resolveInternalTypeName(identifiers(ctx.identifier()));

            if ((internalTypeName != null) && (internalTypeName.charAt(0) != '*'))
                constructorReferenceSet.add(internalTypeName);
        }

        public void enterMemberReferenceExpression(JavaParser.MemberReferenceExpressionContext ctx) {
            // Search "expression '.' identifier" : field
            JavaParser.IdentifierContext identifier = ctx.identifier();

            if (identifier != null) {
                fieldReferenceSet.add(identifier.getText());
            }
        }

        public void enterMethodCall(JavaParser.MethodCallContext ctx) {
            // Search "identifier '(' expressionList? ')'", "this(...)", "super(...)" and "expression '.' identifier '(' expressionList? ')'" : method
            String methodName = ctx.getChild(0).getText();
            methodReferenceSet.add(methodName);
        }

        public void enterLiteral(JavaParser.LiteralContext ctx) {
            TerminalNode stringLiteral = ctx.STRING_LITERAL();
            if (stringLiteral != null) {
                stringSet.add(stringLiteral.getSymbol().getText());
            }
        }
    }
}
