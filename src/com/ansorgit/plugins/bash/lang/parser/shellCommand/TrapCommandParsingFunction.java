package com.ansorgit.plugins.bash.lang.parser.shellCommand;

import com.ansorgit.plugins.bash.lang.parser.BashPsiBuilder;
import com.ansorgit.plugins.bash.lang.parser.Parsing;
import com.ansorgit.plugins.bash.lang.parser.ParsingFunction;
import com.google.common.collect.Sets;
import com.intellij.lang.PsiBuilder;

import java.util.Set;

/**
 * Parsing of the trap command.
 * The syntax is (as documented in "help trap"): <code>trap [-lp] [arg signal_spec ...]</code>
 */
public class TrapCommandParsingFunction implements ParsingFunction {
    private static final Set<String> allowedParams = Sets.newHashSet("-l", "-p", "-lp", "-pl");

    public boolean isValid(BashPsiBuilder builder) {
        return builder.getTokenType() == TRAP_KEYWORD;
    }

    @Override
    public boolean parse(BashPsiBuilder builder) {
        assert isValid(builder);

        PsiBuilder.Marker marker = builder.mark();

        //trap keyword
        builder.advanceLexer();

        boolean listParam = false; //presence of -l
        boolean printParam = false; //presence of -p

        while (builder.eof() && allowedParams.contains(builder.getTokenText())) {
            String tokenText = builder.getTokenText();
            if (tokenText.contains("-l")) {
                listParam = true;
            }

            if (builder.getTokenText().contains("-p")) {
                printParam = true;
            }

            builder.advanceLexer();
        }

        boolean success = true;
        //if -l is given then trap lists all supported signal names, it overrides all other params

        //if -p is given then trap prints either all handlers or the handlers for the optional signal handler

        if (!listParam && !printParam && !builder.eof()) {
            //the next token is the name of a command to attach the signal spec to
            //we need to nest the elements in the same way as BashSimpleCommandImpl
            //fixme improve this handling, if possible
            PsiBuilder.Marker commandMarker = builder.mark();
            PsiBuilder.Marker innerCommandMarker = builder.mark();

            success = Parsing.word.parseWord(builder);

            if (success) {
                innerCommandMarker.done(GENERIC_COMMAND_ELEMENT);
                commandMarker.done(SIMPLE_COMMAND_ELEMENT);
            } else {
                innerCommandMarker.drop();
                commandMarker.drop();
            }
        }

        if (Parsing.word.isWordToken(builder)) {
            success = Parsing.word.parseWordList(builder, false, true);
        }

        if (success) {
            marker.done(TRAP_COMMAND);
        } else {
            marker.drop();
        }

        return success;
    }
}
