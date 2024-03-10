package com.interpreter.lox;

import java.util.List;
import java.util.ArrayList;

class Interpreter implements Expr.Visitor<Object>,
                             Stmt.Visitor<Void> {

  final Environment globals = new Environment();
  private Environment environment = globals;
  private static Object uninitialized = new Object();
  private static boolean toBeContinued = false;
  private static class BreakException extends RuntimeException {}
  private static class ContinueException extends RuntimeException {}
  private static boolean isCorrectType = true;

  Interpreter() {
    globals.define("clock", new LoxCallable() {

      @Override
      public int arity() { return 0; }

      @Override
      public Object call(Interpreter interpreter,
                         List<Object> arguments) {
        return (double)System.currentTimeMillis() / 1000.0;
      }

      @Override
      public String toString() { return "<native fn>"; }
    });

    // REMOVE
    globals.define("abs", new LoxCallable() {

      @Override
      public int arity() { return 1; }

      @Override
      public Object call(Interpreter interpreter,
                         List<Object> arguments) {
        double arg;
        try{
          arg = Double.parseDouble(arguments.get(0).toString());
          if(arg < 0) {
            arg = -1 * arg;
          }
          return arg;
        } catch (NumberFormatException err) {
          isCorrectType = false;
        }
        return null;
      }

      @Override
      public String toString() { return "<native fn>"; }
    });

    globals.define("pow", new LoxCallable() {

      @Override
      public int arity() { return 2; }

      @Override
      public Object call(Interpreter interpreter,
                         List<Object> arguments) {
        double m1;
        double m2;
        try {
          m1 = Double.parseDouble(arguments.get(0).toString());
          m2 = Double.parseDouble(arguments.get(1).toString());
          for (int i = 1; i < m2; i++) {
            m1 += m1;
          }
          return m1;
        } catch (NumberFormatException err) {
          isCorrectType = false;
        }
        return null;
      }

      @Override
      public String toString() { return "<native fn>"; }
    });
    // REMOVE
  }

  void interpret(List<Stmt> statements) {
    try {
      for (Stmt statement : statements) {
        execute(statement);
      }
    } catch (RuntimeError error) {
      Main.runtimeError(error);
    }
  }

  String interpret(Expr expression) {
    try{
        Object value = evaluate(expression);
        return stringify(value);
    } catch (RuntimeError error) {
        Main.runtimeError(error);
        return null;
    }
  }

  @Override
  public Object visitLiteralExpr(Expr.Literal expr) {
    return expr.value;
  }

  @Override
  public Object visitLogicalExpr(Expr.Logical expr) {
    Object left = evaluate(expr.left);
    if (expr.operator.type == TokenType.OR) {
      if (isTruthy(left)) return left;
    } else {
      if (!isTruthy(left)) return left;
    }
    return evaluate(expr.right);
  }

  @Override
  public Object visitGroupingExpr(Expr.Grouping expr) {
    return evaluate(expr.expression);
  }

  @Override
  public Object visitUnaryExpr(Expr.Unary expr) {
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      case BANG:
        return !isTruthy(right);
      case MINUS:
        checkNumberOperand(expr.operator, right);
        return -(double)right;
    }

    return null;
  }

  @Override
  public Object visitVariableExpr(Expr.Variable expr) {
    Object value = environment.get(expr.name);
    if (value == uninitialized) {
        throw new RuntimeError(expr.name,
            "Variable must be initialized before using.");
    }
    return value;
  }

  private void checkNumberOperand(Token operator, Object operand) {
    if (operand instanceof Double) return;
    throw new RuntimeError(operator, "Operand must be a number.");
  }

  private void checkNumberOperands(Token operator, Object left, Object right) {
    if (left instanceof Double && right instanceof Double) return;
    throw new RuntimeError(operator, "Operands must be numbers.");
  }

  private Object evaluate(Expr expr) {
    return expr.accept(this);
  }

  private void execute(Stmt stmt) {
    stmt.accept(this);
  }

  void executeBlock(List<Stmt> statements,
                    Environment environment) {
    Environment previous = this.environment;
    try {
      this.environment = environment;
      for (Stmt statement : statements) {
        if(toBeContinued) {
          toBeContinued = false;
          continue;
        }
        execute(statement);
      }
    } finally {
      this.environment = previous;
    }
  }

  @Override
  public Void visitBlockStmt(Stmt.Block stmt) {
    executeBlock(stmt.statements, new Environment(environment));
    return null;
  }

  @Override
  public Void visitPrintStmt(Stmt.Print stmt) {
    Object value = evaluate(stmt.expression);
    System.out.println(stringify(value));
    return null;
  }

  @Override
  public Void visitReturnStmt(Stmt.Return stmt) {
    Object value = null;
    if (stmt.value != null) value = evaluate(stmt.value);
    throw new Return(value);
  }

  @Override
  public Void visitVarStmt(Stmt.Var stmt) {
    Object value = uninitialized;
    if (stmt.initializer != null) {
      value = evaluate(stmt.initializer);
    }

    environment.define(stmt.name.lexeme, value);
    return null;
  }

  @Override
  public Void visitWhileStmt(Stmt.While stmt) {
    try {
    while (isTruthy(evaluate(stmt.condition))) {
      try {
        execute(stmt.body);
      } catch (ContinueException ex) {
        toBeContinued = true;
      }
    }
    } catch (BreakException ex) {
        // Do nary a thing.
    }
    return null;
  }

  @Override
  public Object visitAssignExpr(Expr.Assign expr) {
    Object value = evaluate(expr.value);
    environment.assign(expr.name, value);
    return value;
  }

  @Override
  public Void visitExpressionStmt(Stmt.Expression stmt) {
    evaluate(stmt.expression);
    return null;
  }

  @Override
  public Void visitFunctionStmt(Stmt.Function stmt) {
    // REMOVE
    String fnName = stmt.name.lexeme;
    environment.define(fnName, new LoxFunction(fnName, stmt.function, environment));
    // REMOVE
    // LoxFunction function = new LoxFunction(stmt, environment);
    // environment.define(stmt.name.lexeme, function);
    return null;
  }

  // REMOVE
  @Override
  public Object visitFunctionExpr(Expr.Function expr) {
    return new LoxFunction(null, expr, environment);
  }
  // REMOVE

  @Override
  public Void visitIfStmt(Stmt.If stmt) {
    if (isTruthy(evaluate(stmt.condition))) {
      execute(stmt.thenBranch);
    } else if (stmt.elseBranch != null) {
      execute(stmt.elseBranch);
    }
    return null;
  }

  @Override
  public Object visitBinaryExpr(Expr.Binary expr) {
    Object left = evaluate(expr.left);
    Object right = evaluate(expr.right);

    switch (expr.operator.type) {
      case GREATER:
        checkNumberOperands(expr.operator, left, right);
        return (double)left > (double)right;
      case GREATER_EQUAL:
        checkNumberOperands(expr.operator, left, right);
        return (double)left >= (double)right;
      case LESS:
        checkNumberOperands(expr.operator, left, right);
        return (double)left < (double)right;
      case LESS_EQUAL:
        checkNumberOperands(expr.operator, left, right);
        return (double)left <= (double)right;
      case MINUS:
        checkNumberOperands(expr.operator, left, right);
        return (double)left - (double)right;
      case PLUS:
        if (left instanceof Double && right instanceof Double) {
          return (double)left + (double)right;
        }
        if (left instanceof String && right instanceof String) {
          return (String)left + (String)right;
        }
        if (left instanceof String && right instanceof Double) {
          double value = (double)right;
          return (String)left + String.valueOf(value);
        }
        if (left instanceof String && right instanceof Boolean) {
          boolean value = (boolean)right;
          return (String)left + Boolean.toString(value);
        }
        if (left instanceof Double && right instanceof String) {
          return String.valueOf((double)left) + (String)right;
        }
        throw new RuntimeError(expr.operator,
          "Operands must be two numbers or two strings.");
      case SLASH:
        checkNumberOperands(expr.operator, left, right);
        return (double)left / (double)right;
      case STAR:
        checkNumberOperands(expr.operator, left, right);
        return (double)left * (double)right;
      case BANG_EQUAL:
        return !isEqual(left, right);
      case EQUAL_EQUAL:
        return isEqual(left, right);
    }

    return null;
  }

  @Override
  public Object visitCallExpr(Expr.Call expr) {
    Object callee = evaluate(expr.callee);
    List<Object> arguments = new ArrayList<>();
    for (Expr argument : expr.arguments) {
      arguments.add(evaluate(argument));
    }

    if (!(callee instanceof LoxCallable)) {
      throw new RuntimeError(expr.paren,
        "Can call only functions and classes.");
    }

    if (!isCorrectType) {
      for(Expr arg : expr.arguments) {
        checkNumberOperand(expr.paren, arg);
      }
    }

    LoxCallable function = (LoxCallable)callee;
    if (arguments.size() != function.arity()) {
      throw new RuntimeError(expr.paren, function + " expected " +
        function.arity() + " arguments but got " +
        arguments.size() + ".");
    }

    return function.call(this, arguments);
  }

  public Object visitConditionalExpr(Expr.Conditional expr) {
    Expr condition = expr.expression;
    if(isTruthy(evaluate(condition))){
        return evaluate(expr.thenBranch);
    } else if (!isTruthy(evaluate(condition)) && expr.elseBranch != null) {
        return evaluate(expr.elseBranch);
    }
    return null;
  }

  @Override
  public Void visitBreakStmt(Stmt.Break stmt) {
    throw new BreakException();
  }

  @Override
  public Void visitContinueStmt(Stmt.Continue stmt) {
    throw new ContinueException();
  }

  private boolean isTruthy(Object object) {
    if (object == null) return false;
    if (object instanceof Boolean) return (boolean)object;
    return true;
  }

  private boolean isEqual(Object a, Object b) {
    if (a == null && b == null) return true;
    if (a == null) return false;
    return a.equals(b);
  }

  private String stringify(Object object) {
    if (object == null) return "nil";
    if (object instanceof Double) {
      String text = object.toString();
      if (text.endsWith(".0")) {
        text = text.substring(0, text.length() - 2);
      }
      return text;
    }
    return object.toString();
  }

}
