package edu.uw.cs.lil.tiny.mr.lambda.visitor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.uw.cs.lil.tiny.mr.lambda.Lambda;
import edu.uw.cs.lil.tiny.mr.lambda.Literal;
import edu.uw.cs.lil.tiny.mr.lambda.LogicLanguageServices;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalConstant;
import edu.uw.cs.lil.tiny.mr.lambda.LogicalExpression;
import edu.uw.cs.lil.tiny.mr.lambda.Variable;
import edu.uw.cs.lil.tiny.mr.language.type.Type;

/**
 * Replace the types of all variables with the most generalized type.
 * 
 * @author Yoav Artzi
 */
public class GeneralizeLambdaVariableTypes implements ILogicalExpressionVisitor {
	
	private final Map<Variable, Variable>	newVariableMapping	= new HashMap<Variable, Variable>();
	
	private LogicalExpression				result				= null;
	
	public static LogicalExpression of(LogicalExpression exp) {
		final GeneralizeLambdaVariableTypes visitor = new GeneralizeLambdaVariableTypes();
		visitor.visit(exp);
		return visitor.result;
	}
	
	@Override
	public void visit(Lambda lambda) {
		final Variable varibleToUse;
		final Type generalizedType = LogicLanguageServices.getTypeRepository()
				.generalizeType(lambda.getArgument().getType());
		if (lambda.getArgument().getType().equals(generalizedType)) {
			varibleToUse = lambda.getArgument();
		} else {
			varibleToUse = new Variable(generalizedType);
			newVariableMapping.put(lambda.getArgument(), varibleToUse);
		}
		lambda.getBody().accept(this);
		if (varibleToUse != lambda.getArgument() || lambda.getBody() != result) {
			result = new Lambda(varibleToUse, result);
		} else {
			result = lambda;
		}
	}
	
	@Override
	public void visit(Literal literal) {
		literal.getPredicate().accept(this);
		final LogicalExpression predicateToUse = result;
		
		boolean argsChanged = false;
		final List<LogicalExpression> newArgs = new ArrayList<LogicalExpression>(
				literal.numArgs());
		for (final LogicalExpression arg : literal.getArguments()) {
			arg.accept(this);
			if (result != arg) {
				argsChanged = true;
			}
			newArgs.add(result);
		}
		
		if (argsChanged || predicateToUse != literal.getPredicate()) {
			result = new Literal(predicateToUse, argsChanged ? newArgs
					: literal.getArguments());
		} else {
			result = literal;
		}
	}
	
	@Override
	public void visit(LogicalConstant logicalConstant) {
		// Nothing to do
		result = logicalConstant;
	}
	
	@Override
	public void visit(LogicalExpression logicalExpression) {
		logicalExpression.accept(this);
	}
	
	@Override
	public void visit(Variable variable) {
		if (newVariableMapping.containsKey(variable)) {
			result = newVariableMapping.get(variable);
		} else {
			result = variable;
		}
	}
	
}