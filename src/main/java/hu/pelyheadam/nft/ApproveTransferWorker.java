package hu.pelyheadam.nft;

import hu.pelyheadam.config.AddressConfig;
import hu.pelyheadam.contract.TestNFT;
import hu.pelyheadam.contract.seats.SeatsToken;
import org.camunda.bpm.client.spring.annotation.ExternalTaskSubscription;
import org.camunda.bpm.client.task.ExternalTask;
import org.camunda.bpm.client.task.ExternalTaskHandler;
import org.camunda.bpm.client.task.ExternalTaskService;
import org.springframework.stereotype.Component;
import org.web3j.crypto.CipherException;
import org.web3j.crypto.Credentials;
import org.web3j.crypto.WalletUtils;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.RemoteFunctionCall;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tx.gas.ContractGasProvider;
import org.web3j.tx.gas.StaticGasProvider;

import java.io.IOException;
import java.math.BigInteger;
import java.util.logging.Logger;

import static hu.pelyheadam.config.SmartContractConfig.GAS_LIMIT;
import static hu.pelyheadam.config.SmartContractConfig.GAS_PRICE;

@Component
@ExternalTaskSubscription("approve-transfer")
public class ApproveTransferWorker implements ExternalTaskHandler {

    private final static Logger LOGGER = Logger.getLogger(ManageContractWorker.class.getName());
    private static final Web3j web3 = Web3j.build(new HttpService("http://vm.niif.cloud.bme.hu:5601"));
    private static final AddressConfig addressPaths = new AddressConfig();

    @Override
    public void execute(ExternalTask externalTask, ExternalTaskService externalTaskService) {

        String from = externalTask.getVariable("_from");
        String contractAddress = externalTask.getVariable("_contractAddress");
        String to = externalTask.getVariable("_to");
        String tokenIdString = externalTask.getVariable("_tokenId");
        BigInteger tokenId = new BigInteger(tokenIdString);

        //LOGGER.info("tokenId: " + tokenId.toString());

        String fromPath = addressPaths.getPathFromAddress(from);

        Credentials credentials = null;
        try {
            credentials = WalletUtils.loadCredentials("", fromPath);
        } catch (IOException | CipherException e) {
            e.printStackTrace();
        }

        ContractGasProvider gasProvider = new StaticGasProvider(GAS_PRICE,GAS_LIMIT);

        SeatsToken contract = SeatsToken.load(contractAddress, web3, credentials, gasProvider);

        TransactionReceipt receipt = null;

        // todo: Cannot invoke "org.web3j.protocol.core.methods.response.TransactionReceipt.getTransactionHash()" because "receipt" is null

        try {
            receipt = contract.approve(to, tokenId).send();
        } catch (Exception e) {
            e.printStackTrace();
        }

        LOGGER.info("Approved! Transaction hash: " + receipt.getTransactionHash());

        externalTaskService.complete(externalTask);
    }
}
