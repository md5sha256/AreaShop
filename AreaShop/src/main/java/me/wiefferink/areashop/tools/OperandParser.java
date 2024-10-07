package me.wiefferink.areashop.tools;

/**
 * Parse math from a string
 * Adapted from:
 * <a href="https://stackoverflow.com/questions/3422673/how-to-evaluate-a-math-expression-given-in-string-form">...</a>
 */
public final class OperandParser {

    private final String str;

    private OperandParser(final String str) {
        this.str = str;
    }

    public static double parse(final String str) throws ParseException {
        return new OperandParser(str).parse();
    }


    private int pos = -1, ch;

    private void nextChar() {
        ch = (++pos < str.length()) ? str.charAt(pos) : -1;
    }

    private boolean eat(int charToEat) {
        while (ch == ' ') nextChar();
        if (ch == charToEat) {
            nextChar();
            return true;
        }
        return false;
    }

    private double parse() throws ParseException {
        nextChar();
        double x = parseExpression();
        if (pos < str.length())
        {
            throw new ParseException("Unexpected: " + (char) ch);
        }
        return x;
    }

    // Grammar:
    // expression = term | expression `+` term | expression `-` term
    // term = factor | term `*` factor | term `/` factor
    // factor = `+` factor | `-` factor | `(` expression `)`
    //        | number | functionName factor | factor `^` factor

    private double parseExpression() throws ParseException {
        double x = parseTerm();
        for (; ; ) {
            if (eat('+')) {
                // addition
                x += parseTerm();
            }
            else if (eat('-')) {
                // subtraction
                x -= parseTerm();
            }
            else return x;
        }
    }

    private double parseTerm() throws ParseException {
        double x = parseFactor();
        for (; ; ) {
            if (eat('*')) {
                // multiplication
                x *= parseFactor();
            }
            else if (eat('/')) {
                // division
                x /= parseFactor();
            }
            else return x;
        }
    }

    private double parseFactor() throws ParseException {
        if (eat('+')) {
            // unary plus
            return parseFactor();
        }
        if (eat('-')) {
            // unary minus
            return -parseFactor();
        }

        double x;
        int startPos = this.pos;
        if (eat('(')) {
            // parentheses
            x = parseExpression();
            eat(')');
        } else if ((ch >= '0' && ch <= '9') || ch == '.') {
            // numbers
            while ((ch >= '0' && ch <= '9') || ch == '.') {
                nextChar();
            }
            x = Double.parseDouble(str.substring(startPos, this.pos));
        } else if (ch >= 'a' && ch <= 'z') {
            // functions
            while (ch >= 'a' && ch <= 'z') {
                nextChar();
            }
            String func = str.substring(startPos, this.pos);
            x = parseFactor();
            x = switch (func) {
                case "sqrt" -> Math.sqrt(x);
                case "sin" -> Math.sin(Math.toRadians(x));
                case "cos" -> Math.cos(Math.toRadians(x));
                case "tan" -> Math.tan(Math.toRadians(x));
                default -> throw new ParseException("Unknown function: " + func);
            };
        } else {
            throw new ParseException("Unexpected: " + (char) ch);
        }

        if (eat('^')) {
            // exponentiation
            x = Math.pow(x, parseFactor());
        }

        return x;
    }

    public static class ParseException extends Exception {

        public ParseException() {
        }

        public ParseException(String message) {
            super(message);
        }

        public ParseException(String message, Throwable cause) {
            super(message, cause);
        }

        public ParseException(Throwable cause) {
            super(cause);
        }

        public ParseException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace) {
            super(message, cause, enableSuppression, writableStackTrace);
        }
    }

}
