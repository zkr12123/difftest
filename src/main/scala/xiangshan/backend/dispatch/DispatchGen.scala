package xiangshan.backend.dispatch

import chisel3._
import chisel3.util._
import xiangshan._
import xiangshan.utils.{XSDebug}

class DispatchGen extends XSModule {
  val io = IO(new Bundle() {
    // from dispatch queues
    val fromIntDq = Flipped(Vec(IntDqDeqWidth, ValidIO(new MicroOp)))
    val fromFpDq = Flipped(Vec(FpDqDeqWidth, ValidIO(new MicroOp)))
    val fromLsDq = Flipped(Vec(LsDqDeqWidth, ValidIO(new MicroOp)))

    // enq Issue Queue
    val numExist = Input(Vec(exuConfig.ExuCnt, UInt(log2Ceil(IssQueSize).W)))
    val enqIQIndex = Vec(exuConfig.ExuCnt, ValidIO(UInt(log2Ceil(IntDqDeqWidth).W)))
  })

  assert(IntDqDeqWidth >= FpDqDeqWidth)
  assert(IntDqDeqWidth >= LsDqDeqWidth)

  def genIQIndex(exunum: Int, deqnum: Int, deq: Seq[Bool], numExist: Seq[UInt]) = {
    assert(isPow2(deqnum))
    // index without priority
    val IQIndex = Wire(Vec(exunum, UInt((log2Ceil(deqnum) + 1).W)))
    var last_deq = deq
    for (i <- 0 until exunum) {
      IQIndex(i) := PriorityEncoder(last_deq :+ true.B)
      val onehot = UIntToOH(IQIndex(i))
      last_deq = (0 until deqnum).map(i => !onehot(i) && last_deq(i))
    }
    // now consider the IQ priority with numExist
//    var currMax = (0 until numExist.length)
    IQIndex
  }

  val bruIQIndex = genIQIndex(exuConfig.BruCnt, IntDqDeqWidth, io.fromIntDq.map(_.bits.ctrl.fuType === FuType.bru),
    (0 until exuConfig.BruCnt).map(i => io.numExist(i)))
  val aluIQIndex = genIQIndex(exuConfig.AluCnt, IntDqDeqWidth, io.fromIntDq.map(_.bits.ctrl.fuType === FuType.alu),
    (0 until exuConfig.AluCnt).map(i => io.numExist(exuConfig.BruCnt+i)))
  val mulIQIndex = genIQIndex(exuConfig.MulCnt, IntDqDeqWidth, io.fromIntDq.map(_.bits.ctrl.fuType === FuType.mul),
    (0 until exuConfig.MulCnt).map(i => io.numExist(exuConfig.BruCnt+exuConfig.AluCnt+i)))
  val muldivIQIndex = genIQIndex(exuConfig.MduCnt, IntDqDeqWidth, io.fromIntDq.map(_.bits.ctrl.fuType === FuType.mdu),
    (0 until exuConfig.MduCnt).map(i => io.numExist(exuConfig.BruCnt+exuConfig.AluCnt+exuConfig.MulCnt+i)))
  val fmacIQIndex = genIQIndex(exuConfig.FmacCnt, FpDqDeqWidth, io.fromFpDq.map(_.bits.ctrl.fuType === FuType.fmac),
    (0 until exuConfig.FmacCnt).map(i => io.numExist(exuConfig.IntExuCnt+i)))
  val fmiscIQIndex = genIQIndex(exuConfig.FmiscCnt, FpDqDeqWidth, io.fromFpDq.map(_.bits.ctrl.fuType === FuType.fmisc),
    (0 until exuConfig.FmiscCnt).map(i => io.numExist(exuConfig.IntExuCnt+exuConfig.FmacCnt+i)))
  val lduIQIndex = genIQIndex(exuConfig.LduCnt, LsDqDeqWidth, io.fromLsDq.map(_.bits.ctrl.fuType === FuType.ldu),
    (0 until exuConfig.LduCnt).map(i => io.numExist(exuConfig.IntExuCnt+exuConfig.FpExuCnt+i)))
//  val stuIQIndex = genIQIndex(exuConfig.StuCnt, LsDqDeqWidth, io.fromLsDq.map(_.bits.ctrl.fuType === FuType.stu))
  val stuIQIndex = genIQIndex(exuConfig.StuCnt, LsDqDeqWidth, io.fromLsDq.map(deq => FuType.isMemExu(deq.bits.ctrl.fuType)),
    (0 until exuConfig.StuCnt).map(i => io.numExist(exuConfig.IntExuCnt+exuConfig.FpExuCnt+exuConfig.LduCnt+i)))

  val allIndex = Seq(bruIQIndex, aluIQIndex, mulIQIndex, muldivIQIndex,
    fmacIQIndex, fmiscIQIndex,
    lduIQIndex, stuIQIndex
  )
  val allCnt = Seq(exuConfig.BruCnt, exuConfig.AluCnt, exuConfig.MulCnt, exuConfig.MduCnt,
    exuConfig.FmacCnt, exuConfig.FmiscCnt,
    exuConfig.LduCnt, exuConfig.StuCnt
  )
  assert(allIndex.length == allCnt.length)
  var startIndex = 0
  for (i <- 0 until allIndex.length) {
    for (j <- 0 until allCnt(i)) {
      io.enqIQIndex(startIndex + j).valid := !allIndex(i)(j)(2)
      io.enqIQIndex(startIndex + j).bits := allIndex(i)(j)(1, 0)
    }
    startIndex += allCnt(i)
  }
}
