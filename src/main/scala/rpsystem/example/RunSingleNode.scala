package rpsystem.example

import java.util.UUID
import java.util.Date
import java.math.BigDecimal
import java.util.Scanner
import rpsystem.system._
import rpsystem.domain._
import rpsystem.util.AccountAggrFactory

object RunSingleNode {
  def main(args: Array[String]): Unit = {
    val system = new SingleNodeSystem()
    system.createDefaultCappedCollection
    system.setEvtRecoveryAvailable(false)
    system.setCmdRecoveryAvailable(false)
    
    //system.setShardingsNumber(10)
    //system.setCmdBusMode(CommandBus.SHARDINGS_MODE_MOD_ACCOUTID(10))
    
    //system.setCmdHdlMode(CommandHandler.SNAPSHOT_MODE_EVENTSNUM(5, 0))
    //system.setCmdHdlMode(CommandHandler.SNAPSHOT_MODE_MILLISECOND(60000, new java.util.Date().getTime()))
    
    system.initial
    
    val scanner = new Scanner(System.in)
    Thread.sleep(2000)
    print("Create 10000 accounts example? (y/n)")
    if (scanner.next() == "y"){
      var accountIds:scala.collection.mutable.ArrayBuffer[UUID] = new scala.collection.mutable.ArrayBuffer[UUID]()
      for(i <- 1 to 10000){
        val idStr: String = "550E8400-E29B-11D4-A716-4466" + (i + 10000000).toString        
        val id:UUID = java.util.UUID.fromString(idStr)
        accountIds = accountIds :+ id      
        system.addAccountSnapshot(AccountAggrFactory.getAccountAggr(id, "Name"+i, "RMB", new BigDecimal(10000)))
      }
      print("create accounts successfully")
    }else{
      print("Select 'not to create'.")
    }
    
    var i = 1
    while(true) {
      val idStr: String = "550E8400-E29B-11D4-A716-4466" + (i + 10000000).toString        
      val id:UUID = java.util.UUID.fromString(idStr)
      
      val cmd:DepositCommand = new DepositCommand(UUID.randomUUID(), new Date(), id, new BigDecimal(1), new BigDecimal(1))
      //val cmd:WithdrawCommand = new WithdrawCommand(UUID.randomUUID(), new Date(), id, new BigDecimal(1), new BigDecimal(1))
      //val idStr2: String = "550E8400-E29B-11D4-A716-4466" + (10000 - i + 10000000).toString     
      //val id2:UUID = java.util.UUID.fromString(idStr2)
      //val cmd:TransferInCommand = new TransferInCommand(UUID.randomUUID(), new Date(), id, new BigDecimal(1), id2, new BigDecimal(1))
      //val cmd2:TransferOutCommand = new TransferOutCommand(UUID.randomUUID(), new Date(), id2, new BigDecimal(1), id, new BigDecimal(1))
      
      system.sendCommand(cmd)
      //system.sendCommand(cmd2)
      i += 1
      if(i == 10001)
        i=1
      if(i % 3 == 0) //change the frequency
        Thread.sleep(1)
    }
    
    Thread.sleep(2000)
	system.shutdown()
  }
}