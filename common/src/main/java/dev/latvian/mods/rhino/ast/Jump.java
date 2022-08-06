/* -*- Mode: java; tab-width: 8; indent-tabs-mode: nil; c-basic-offset: 4 -*-
 *
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package dev.latvian.mods.rhino.ast;

import dev.latvian.mods.rhino.Node;
import dev.latvian.mods.rhino.Token;

/**
 * Used for code generation.  During codegen, the AST is transformed
 * into an Intermediate Representation (IR) in which loops, ifs, switches
 * and other control-flow statements are rewritten as labeled jumps.
 * If the parser is set to IDE-mode, the resulting AST will not contain
 * any instances of this class.
 */
public class Jump extends AstNode {

	public Node target;
	private Node target2;
	private Jump jumpNode;

	public Jump() {
		type = Token.ERROR;
	}

	public Jump(int nodeType) {
		type = nodeType;
	}

	public Jump(int type, int lineno) {
		this(type);
		setLineno(lineno);
	}

	public Jump(int type, Node child) {
		this(type);
		addChildToBack(child);
	}

	public Jump(int type, Node child, int lineno) {
		this(type, child);
		setLineno(lineno);
	}

	public Jump getJumpStatement() {
		if (type != Token.BREAK && type != Token.CONTINUE) {
			throw codeBug();
		}
		return jumpNode;
	}

	public void setJumpStatement(Jump jumpStatement) {
		if (type != Token.BREAK && type != Token.CONTINUE) {
			throw codeBug();
		}
		if (jumpStatement == null) {
			throw codeBug();
		}
		if (this.jumpNode != null) {
			throw codeBug(); //only once
		}
		this.jumpNode = jumpStatement;
	}

	public Node getDefault() {
		if (type != Token.SWITCH) {
			throw codeBug();
		}
		return target2;
	}

	public void setDefault(Node defaultTarget) {
		if (type != Token.SWITCH) {
			throw codeBug();
		}
		if (defaultTarget.getType() != Token.TARGET) {
			throw codeBug();
		}
		if (target2 != null) {
			throw codeBug(); //only once
		}
		target2 = defaultTarget;
	}

	public Node getFinally() {
		if (type != Token.TRY) {
			throw codeBug();
		}
		return target2;
	}

	public void setFinally(Node finallyTarget) {
		if (type != Token.TRY) {
			throw codeBug();
		}
		if (finallyTarget.getType() != Token.TARGET) {
			throw codeBug();
		}
		if (target2 != null) {
			throw codeBug(); //only once
		}
		target2 = finallyTarget;
	}

	public Jump getLoop() {
		if (type != Token.LABEL) {
			throw codeBug();
		}
		return jumpNode;
	}

	public void setLoop(Jump loop) {
		if (type != Token.LABEL) {
			throw codeBug();
		}
		if (loop == null) {
			throw codeBug();
		}
		if (jumpNode != null) {
			throw codeBug(); //only once
		}
		jumpNode = loop;
	}

	public Node getContinue() {
		if (type != Token.LOOP) {
			throw codeBug();
		}
		return target2;
	}

	public void setContinue(Node continueTarget) {
		if (type != Token.LOOP) {
			throw codeBug();
		}
		if (continueTarget.getType() != Token.TARGET) {
			throw codeBug();
		}
		if (target2 != null) {
			throw codeBug(); //only once
		}
		target2 = continueTarget;
	}

	/**
	 * Jumps are only used directly during code generation, and do
	 * not support this interface.
	 *
	 * @throws UnsupportedOperationException
	 */
	@Override
	public void visit(NodeVisitor visitor) {
		throw new UnsupportedOperationException(this.toString());
	}
}
