package scorex.transaction

import com.wavesplatform.{TransactionGen, WithDB}
import com.wavesplatform.history.{DefaultWavesSettings, StorageFactory}
import com.wavesplatform.state2.HistoryTest
import org.scalatest.{Matchers, PropSpec}
import org.scalatest.prop.PropertyChecks
import scorex.lagonaki.mocks.TestBlock
import scorex.transaction.TransactionParser.TransactionType
import scorex.transaction.assets.SmartIssueTransaction

class SmartIssueTransactionSpecification extends PropSpec with PropertyChecks with Matchers with TransactionGen with WithDB with HistoryTest {

  property("SmartIssueTransaction serialization roundtrip") {
    forAll(smartIssueTransactionGen) { tx: SmartIssueTransaction =>
      require(tx.bytes().head == TransactionType.SmartIssueTransaction.id)
      val recovered = SmartIssueTransaction.parseTail(tx.bytes().tail).get

      tx.sender.address shouldEqual recovered.sender.address
      tx.timestamp shouldEqual recovered.timestamp
      tx.decimals shouldEqual recovered.decimals
      tx.description shouldEqual recovered.description
      tx.script shouldEqual recovered.script
      tx.reissuable shouldEqual recovered.reissuable
      tx.fee shouldEqual recovered.fee
      tx.name shouldEqual recovered.name
      tx.chainId shouldEqual recovered.chainId
      tx.bytes() shouldEqual recovered.bytes()
    }
  }

  property("assets are added to state") {
    val (h, fp, reader, bu, _) = StorageFactory(db, DefaultWavesSettings).get._1()
    bu.processBlock(genesisBlock)

    forAll(smartIssueTransactionGen) { tx: SmartIssueTransaction =>
      bu.processBlock(TestBlock.create(Seq(tx)))
      reader().assetInfo(tx.id()).get.script shouldEqual tx.script
    }





  }

}
