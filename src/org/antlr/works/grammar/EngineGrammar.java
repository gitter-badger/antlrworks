/*

[The "BSD licence"]
Copyright (c) 2005 Jean Bovet
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions
are met:

1. Redistributions of source code must retain the above copyright
notice, this list of conditions and the following disclaimer.
2. Redistributions in binary form must reproduce the above copyright
notice, this list of conditions and the following disclaimer in the
documentation and/or other materials provided with the distribution.
3. The name of the author may not be used to endorse or promote products
derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE AUTHOR ``AS IS'' AND ANY EXPRESS OR
IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY DIRECT, INDIRECT,
INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.

*/


package org.antlr.works.grammar;

import antlr.RecognitionException;
import antlr.TokenStreamException;
import org.antlr.Tool;
import org.antlr.analysis.DecisionProbe;
import org.antlr.analysis.NFAState;
import org.antlr.tool.ErrorManager;
import org.antlr.tool.Grammar;
import org.antlr.tool.GrammarNonDeterminismMessage;
import org.antlr.tool.GrammarUnreachableAltsMessage;
import org.antlr.works.ate.syntax.misc.ATEToken;
import org.antlr.works.components.grammar.CEditorGrammar;
import org.antlr.works.syntax.GrammarSyntaxName;
import org.antlr.works.syntax.GrammarSyntaxRule;
import org.antlr.works.utils.ErrorListener;

import javax.swing.*;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class EngineGrammar {

    protected Grammar parserGrammar;
    protected Grammar lexerGrammar;
    protected List errors;

    protected boolean grammarDirty;
    protected boolean grammarAnalyzeDirty;

    protected CEditorGrammar editor;

    public EngineGrammar(CEditorGrammar editor) {
        this.editor = editor;
        errors = new ArrayList();
        makeDirty();
    }

    public void makeDirty() {
        grammarDirty = true;
        grammarAnalyzeDirty = true;
    }

    public boolean isDirty() {
        return grammarDirty || grammarAnalyzeDirty;
    }

    public Grammar getParserGrammar() throws Exception {
        createGrammars();
        return parserGrammar;
    }

    public Grammar getLexerGrammar() throws Exception {
        createGrammars();
        return lexerGrammar;
    }

    public NFAState getRuleStartState(String name) throws Exception {
        Grammar g;

        if(ATEToken.isLexerName(name))
            g = getLexerGrammar();
        else
            g = getParserGrammar();

        return g == null ? null:g.getRuleStartState(name);
    }

    public Grammar getGrammarForRule(String name) throws Exception {
        if(ATEToken.isLexerName(name))
            return getLexerGrammar();
        else
            return getParserGrammar();
    }

    public List getErrors() {
        return errors;
    }

    public boolean hasGrammar() {
        switch(getType()) {
            case GrammarSyntaxName.COMBINED:
                return parserGrammar != null;
            case GrammarSyntaxName.TREEPARSER:
            case GrammarSyntaxName.PARSER:
                return parserGrammar != null;
            case GrammarSyntaxName.LEXER:
                return lexerGrammar != null;
        }
        return false;
    }

    public Grammar getANTLRGrammar() {
        switch(getType()) {
            case GrammarSyntaxName.COMBINED:
                return parserGrammar;
            case GrammarSyntaxName.TREEPARSER:
            case GrammarSyntaxName.PARSER:
                return parserGrammar;
            case GrammarSyntaxName.LEXER:
                return lexerGrammar;
        }
        return null;
    }

    public Tool getANTLRTool() {
        if(editor.getFileFolder() != null)
            return new Tool(new String[] { "-lib", editor.getFileFolder() } );
        else
            return new Tool();
    }

    public String getName() {
        GrammarSyntaxName name = editor.parserEngine.getName();
        if(name == null)
            return null;
        else
            return name.name;
    }

    public int getType() {
        GrammarSyntaxName name = editor.parserEngine.getName();
        if(name == null)
            return GrammarSyntaxName.COMBINED;
        else
            return name.getType();
    }

    public void createGrammars() throws Exception {
        if(!grammarDirty)
            return;

        ErrorManager.setErrorListener(ErrorListener.shared());

        switch(getType()) {
            case GrammarSyntaxName.COMBINED:
                createCombinedGrammar();
                break;
            case GrammarSyntaxName.TREEPARSER:
            case GrammarSyntaxName.PARSER:
                createParserGrammar();
                break;
            case GrammarSyntaxName.LEXER:
                createLexerGrammar();
                break;
        }

        grammarDirty = false;
    }

    protected String getFileName() {
        String fileName = editor.getFileName();
        return fileName==null?"<notsaved>":fileName;
    }

    protected Grammar createNewGrammar(String filename, String content) throws TokenStreamException, RecognitionException {
        Grammar g = new Grammar();
        g.setTool(getANTLRTool());
        g.setFileName(filename);
        g.setGrammarContent(content);
        return g;
    }

    protected void createCombinedGrammar() throws Exception {
        parserGrammar = createNewGrammar(getFileName(), editor.getText());
        parserGrammar.createNFAs();
        lexerGrammar = createLexerGrammarFromCombinedGrammar(parserGrammar);
    }

    protected Grammar createLexerGrammarFromCombinedGrammar(Grammar grammar) throws Exception {
        String lexerGrammarStr = grammar.getLexerGrammar();
        if(lexerGrammarStr == null)
            return null;

        Grammar lexerGrammar = new Grammar();
        lexerGrammar.setTool(getANTLRTool());
        lexerGrammar.setFileName("<internally-generated-lexer>");
        lexerGrammar.importTokenVocabulary(grammar);

        lexerGrammar.setGrammarContent(lexerGrammarStr);
        lexerGrammar.createNFAs();

        return lexerGrammar;
    }

    protected void createParserGrammar() throws TokenStreamException, RecognitionException {
        parserGrammar = createNewGrammar(getFileName(), editor.getText());
        parserGrammar.createNFAs();
    }

    protected void createLexerGrammar() throws TokenStreamException, RecognitionException {
        lexerGrammar = createNewGrammar(getFileName(), editor.getText());
        lexerGrammar.createNFAs();
    }

    public void analyze() throws Exception {
        createGrammars();

        if(!grammarAnalyzeDirty)
            return;

        boolean oldVerbose = DecisionProbe.verbose;
        DecisionProbe.verbose = true;

        ErrorManager.setErrorListener(ErrorListener.shared());

        try {
            ErrorListener.shared().clear();
            getANTLRGrammar().createLookaheadDFAs();
            if(getType() == GrammarSyntaxName.COMBINED) {
                // If the grammar is combined, analyze also the lexer
                Grammar lexer = getLexerGrammar();
                if(lexer != null)
                    lexer.createLookaheadDFAs();
            }

            buildNonDeterministicErrors();
            markRulesWithWarningsOrErrors();
        } finally {
            DecisionProbe.verbose = oldVerbose;
        }

        if(SwingUtilities.isEventDispatchThread()) {
            editor.engineGrammarDidAnalyze();
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    editor.engineGrammarDidAnalyze();
                }
            });
        }

        grammarAnalyzeDirty = false;
    }

    protected void buildNonDeterministicErrors() {
        errors.clear();
        for (Iterator iterator = ErrorListener.shared().warnings.iterator(); iterator.hasNext();) {
            Object o = iterator.next();

            if(o instanceof GrammarUnreachableAltsMessage)
                errors.add(buildUnreachableAltsError((GrammarUnreachableAltsMessage)o));
            else if(o instanceof GrammarNonDeterminismMessage)
                errors.add(buildNonDeterministicError((GrammarNonDeterminismMessage)o));
        }
    }

    protected EngineGrammarError buildNonDeterministicError(GrammarNonDeterminismMessage message) {
        EngineGrammarError error = new EngineGrammarError();

        List nonDetAlts = message.probe.getNonDeterministicAltsForState(message.problemState);
        error.setLine(message.probe.dfa.getDecisionASTNode().getLine()-1);

        Set disabledAlts = message.probe.getDisabledAlternatives(message.problemState);
        List labels = message.probe.getSampleNonDeterministicInputSequence(message.problemState);
        String input = message.probe.getInputSequenceDisplay(labels);
        error.setMessage("Decision can match input such as \""+input+"\" using multiple alternatives");

        int firstAlt = 0;

        for (Iterator iter = nonDetAlts.iterator(); iter.hasNext();) {
            Integer displayAltI = (Integer) iter.next();
            NFAState nfaStart = message.probe.dfa.getNFADecisionStartState();

            int tracePathAlt = nfaStart.translateDisplayAltToWalkAlt(message.probe.dfa, displayAltI.intValue());
            if ( firstAlt == 0 )
                firstAlt = tracePathAlt;

            List path =
                message.probe.getNFAPathStatesForAlt(firstAlt,
                                                       tracePathAlt,
                                                       labels);
            error.addPath(path, disabledAlts.contains(displayAltI));
            error.addStates(path);

            // Find all rules enclosing each state (because a path can extend over multiple rules)
            for (Iterator iterator = path.iterator(); iterator.hasNext();) {
                NFAState state = (NFAState)iterator.next();
                error.addRule(state.getEnclosingRule());
            }
        }

        return error;
    }

    protected EngineGrammarError buildUnreachableAltsError(GrammarUnreachableAltsMessage message) {
        EngineGrammarError error = new EngineGrammarError();

        error.setLine(message.probe.dfa.getDecisionASTNode().getLine()-1);
        error.setMessage("The following alternatives are unreachable: "+message.alts);

        NFAState state = message.probe.dfa.getNFADecisionStartState();
        for(int alt=0; alt<message.alts.size(); alt++) {
            error.addUnreachableAlt(state, (Integer)message.alts.get(alt));
            error.addStates(state);
            error.addRule(state.getEnclosingRule());
        }

        return error;
    }

    protected void markRulesWithWarningsOrErrors() throws Exception {
        // Clear graphic cache because we have to redraw each rule again
        editor.visual.clearCacheGraphs();
        for (Iterator iterator = editor.getParserEngine().getRules().iterator(); iterator.hasNext();) {
            GrammarSyntaxRule rule = (GrammarSyntaxRule)iterator.next();
            updateRuleWithErrors(rule, fetchErrorsForRule(rule));
        }

        editor.rules.refreshRules();
    }

    protected void updateRuleWithErrors(GrammarSyntaxRule rule, List errors) throws Exception {
        rule.setErrors(errors);
        editor.visual.createGraphsForRule(rule);
    }

    protected List fetchErrorsForRule(GrammarSyntaxRule rule) {
        List errors = new ArrayList();
        for (Iterator iterator = getErrors().iterator(); iterator.hasNext();) {
            EngineGrammarError error = (EngineGrammarError) iterator.next();
            if(error.line>=rule.start.startLineNumber && error.line<=rule.end.startLineNumber)
                errors.add(error);
        }
        return errors;
    }

}