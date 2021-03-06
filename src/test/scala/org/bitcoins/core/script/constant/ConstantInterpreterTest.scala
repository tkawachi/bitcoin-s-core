package org.bitcoins.core.script.constant

import org.bitcoins.core.script.ScriptProgram
import org.bitcoins.core.script.bitwise.OP_EQUAL
import org.bitcoins.core.script.crypto.OP_CHECKMULTISIGVERIFY
import org.bitcoins.core.script.flag.{ScriptFlag, ScriptVerifyMinimalData}
import org.bitcoins.core.script.result.{ScriptErrorBadOpCode, ScriptErrorMinimalData}
import org.bitcoins.core.util.{ScriptProgramTestUtil, TestUtil}
import org.scalatest.{FlatSpec, MustMatchers}

/**
 * Created by chris on 1/24/16.
 */
class ConstantInterpreterTest extends FlatSpec with MustMatchers with ConstantInterpreter {

  "ConstantInterpreter" must "interpret OP_PUSHDATA1 correctly" in {
    val byteConstantSize = 76
    val byteConstant = for { x <- 0 until byteConstantSize} yield 0x0.toByte
    val scriptConstant = ScriptConstant(byteConstant)
    val stack = List()
    val script = List(OP_PUSHDATA1,ScriptNumber(byteConstantSize), scriptConstant,OP_7,OP_EQUAL)
    val program = ScriptProgram(TestUtil.testProgramExecutionInProgress, stack,script)
    val newProgram = opPushData1(program)
    newProgram.stack must be (List(scriptConstant))
    newProgram.script must be (List(OP_7,OP_EQUAL))
  }

  it must "interpret OP_PUSHDATA2 correctly" in {
    val byteConstantSize = 256
    val byteConstant = for { x <- 0 until byteConstantSize} yield 0x0.toByte
    val scriptConstant = ScriptConstant(byteConstant)
    val stack = List()
    val script = List(OP_PUSHDATA2, ScriptNumber(256), scriptConstant, OP_8, OP_EQUAL)
    val program = ScriptProgram(TestUtil.testProgramExecutionInProgress, stack,script)
    val newProgram = opPushData2(program)
    newProgram.stack must be (List(scriptConstant))
    newProgram.script must be (List(OP_8,OP_EQUAL))
  }

  it must "interpret OP_PUSHDATA4 correctly" in {
    val byteConstantSize = 65536
    val byteConstant = for { x <- 0 until byteConstantSize} yield 0x0.toByte
    val scriptConstant = ScriptConstant(byteConstant)
    val stack = List()
    val script = List(OP_PUSHDATA4, ScriptNumber(byteConstantSize), scriptConstant, OP_9, OP_EQUAL)
    val program = ScriptProgram(TestUtil.testProgramExecutionInProgress, stack,script)
    val newProgram = opPushData4(program)
    newProgram.stack must be (List(scriptConstant))
    newProgram.script must be (List(OP_9, OP_EQUAL))
  }


  it must "push a constant 2 bytes onto the stack" in {
    val stack = List()
    val script = List(BytesToPushOntoStack(2), ScriptNumber.one, OP_0)
    val program = ScriptProgram(TestUtil.testProgram, stack,script)
    val newProgram = pushScriptNumberBytesToStack(program)
    newProgram.script.isEmpty must be (true)
    newProgram.stack must be (List(ScriptConstant("0100")))
  }

  it must "push 0 bytes onto the stack which is OP_0" in {
    val stack = List()
    val script = List(OP_PUSHDATA1,BytesToPushOntoStack(0))
    val program = ScriptProgram(ScriptProgram(TestUtil.testProgram, stack,script),Seq[ScriptFlag]())
    val newProgram  = opPushData1(program)
    newProgram.stackTopIsFalse must be (true)
    newProgram.stack must be (List(ScriptNumber.zero))

    val stack1 = List()
    val script1 = List(OP_PUSHDATA2,BytesToPushOntoStack(0))
    val program1 = ScriptProgram(ScriptProgram(TestUtil.testProgram, stack1,script1),Seq[ScriptFlag]())
    val newProgram1  = opPushData2(program1)
    newProgram1.stack must be (List(ScriptNumber.zero))

    val stack2 = List()
    val script2 = List(OP_PUSHDATA4,BytesToPushOntoStack(0))
    val program2 = ScriptProgram(ScriptProgram(TestUtil.testProgram, stack2,script2),Seq[ScriptFlag]())
    val newProgram2 = opPushData4(program2)
    newProgram2.stack must be (List(ScriptNumber.zero))
  }

  it must "mark a program as invalid if we have do not have enough bytes to be pushed onto the stack by the push operation" in {
    val stack = List()
    val script = List(OP_PUSHDATA1,BytesToPushOntoStack(1))
    val program = ScriptProgram(ScriptProgram(TestUtil.testProgramExecutionInProgress, stack,script),Seq[ScriptFlag]())

    val newProgram  = ScriptProgramTestUtil.toExecutedScriptProgram(opPushData1(program))
    newProgram.error must be (Some(ScriptErrorBadOpCode))
  }

  it must "fail the require statement if the first op_code in the program's script doesn't match the OP_PUSHDATA we're looking for" in {
    val stack1 = List()
    val script1 = List(OP_PUSHDATA1,BytesToPushOntoStack(0))
    val program1 = ScriptProgram(ScriptProgram(TestUtil.testProgram, stack1,script1),Seq[ScriptFlag]())

    val stack2 = List()
    val script2 = List(OP_PUSHDATA2,BytesToPushOntoStack(0))
    val program2 = ScriptProgram(ScriptProgram(TestUtil.testProgram, stack2,script2),Seq[ScriptFlag]())

    val stack4 = List()
    val script4 = List(OP_PUSHDATA4,BytesToPushOntoStack(0))
    val program4 = ScriptProgram(ScriptProgram(TestUtil.testProgram, stack4,script4),Seq[ScriptFlag]())

    //purposely call incorrect functions to mismatch opCodes
    intercept[IllegalArgumentException] {
      opPushData1(program2)
    }

    intercept[IllegalArgumentException] {
      opPushData2(program4)
    }

    intercept[IllegalArgumentException] {
      opPushData4(program1)
    }
  }

  it must "throw exception when parsing bytes need for a push op for a script token other than" +
    "BytesToPushOntoStack, ScriptNumber, or ScriptConstant" in {
    val stack = List()
    val script = List(OP_CHECKMULTISIGVERIFY, ScriptNumber.one, OP_0)
    val program = ScriptProgram(TestUtil.testProgram, stack,script)

    intercept[IllegalArgumentException] {
      pushScriptNumberBytesToStack(program)
    }
  }

  it must "return ScriptErrorMinimalData if program contains ScriptVerifyMinimalData flag and 2nd item in script is" +
    " zero" in {
    val stack = List()
    val script = List(OP_PUSHDATA4,ScriptNumber.zero)
    val program = ScriptProgram(ScriptProgram(TestUtil.testProgram, stack,script),Seq[ScriptFlag](ScriptVerifyMinimalData))
    val newProgram = ScriptProgramTestUtil.toExecutedScriptProgram(opPushData4(program))
    newProgram.error must be (Some(ScriptErrorMinimalData))
  }
}
