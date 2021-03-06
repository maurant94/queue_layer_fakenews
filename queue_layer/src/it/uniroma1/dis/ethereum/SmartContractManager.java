package it.uniroma1.dis.ethereum;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import org.web3j.crypto.Credentials;
import org.web3j.protocol.Web3j;
import org.web3j.protocol.core.methods.response.TransactionReceipt;
import org.web3j.protocol.http.HttpService;
import org.web3j.tuples.generated.Tuple4;
import org.web3j.tuples.generated.Tuple5;
import org.web3j.tx.Contract;
import org.web3j.tx.ManagedTransaction;

public class SmartContractManager {

	private FiveW contract = null;
	private Whitelist contractVote = null;
	
	public static final BigInteger ACK_VOTE = new BigInteger("1");
	public static final BigInteger NACK_VOTE = new BigInteger("0");

	public SmartContractManager() {
		String addressVote = "0x8659e544dd138c2ebf3cdfcb4ee44e1d1927ab4f";
		String address = "0xb02cd1e06ac6e3dd6ed32bde4c7f78a17f2a2f98"; //FIXME AT THE END WE DEPLOY ONE TIME SO WE KNOW THE ADDRESS
		String keyPass = "5c71e8cfae6e0cb8c80602d2f1fc66d1fca5674dd6f2ff05b4908c0156c777c5";
		Web3j web3j = Web3j.build(new HttpService("http://localhost:7545")); //GANACHE
		Credentials credentials = Credentials.create(keyPass);

//		try {
//			contract = FiveW.deploy( web3j, credentials, 
//					ManagedTransaction.GAS_PRICE, Contract.GAS_LIMIT).send();
//			contractVote = Whitelist.deploy( web3j, credentials, 
//					ManagedTransaction.GAS_PRICE, Contract.GAS_LIMIT).send();
//			System.out.println(contract.getContractAddress());
//			System.out.println(contractVote.getContractAddress());
//		} catch (Exception e1) {
//			contract = null;
//			e1.printStackTrace();
//		}
		
		try {
			contract = FiveW.load(address, web3j, credentials, 
					ManagedTransaction.GAS_PRICE, Contract.GAS_LIMIT);
			contractVote = Whitelist.load(addressVote, web3j, credentials, 
					ManagedTransaction.GAS_PRICE, Contract.GAS_LIMIT);
			System.out.println("LOADED");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	/*
	 * METODH OF 1L, INIT OF TRANSACTION PARAMETERS
	 */
	public void start5w(String name, String hash, byte[] payloadRes, String claims, List<it.uniroma1.dis.block.FiveW> fivew) throws Exception {
		System.out.println("START START5W...");
		//bytes
		byte[] payload = new byte[1]; //1 is len
		List<byte[]> bytes = new ArrayList<>();
		for (int i = 0; i < payloadRes.length; i++) {
			payload[0] = payloadRes[i];
			bytes.add(payload);
		}
		//list5w
		String meta5w = ""; //WHERE WHEN WHO DATIVE WHAT order
		List<BigInteger> accuracies = new ArrayList<>();
		String val;
		for (it.uniroma1.dis.block.FiveW fw : fivew) {
			meta5w += fw.getWhere()==null?" ":fw.getWhere().getName()!=null?fw.getWhere().getName():" ";
			meta5w += "#+#";
			meta5w += fw.getWhen()==null?" ":fw.getWhen().getName()!=null?fw.getWhen().getName():" ";
			meta5w += "#+#";
			meta5w += fw.getWho()==null?" ":fw.getWho().getName()!=null?fw.getWho().getName():" ";
			meta5w += "#+#";
			meta5w += fw.getDative()==null?" ":fw.getDative().getName()!=null?fw.getDative().getName():" ";
			meta5w += "#+#";
			meta5w += fw.getWhat()==null?" ":fw.getWhat().getName()!=null?fw.getWhat().getName():" ";
			meta5w += "#+#";
			meta5w += "#-#"; //OR REMOVE AT THE END SENT
			
			val = fw.getWhere()==null?"0":fw.getWhere().getAccuracy()!=null?((long)(fw.getWhere().getAccuracy()*1000)+""):" ";
			accuracies.add(new BigInteger(val));
			val = fw.getWhen()==null?"0":fw.getWhen().getAccuracy()!=null?((long)(fw.getWhen().getAccuracy()*1000)+""):" ";
			accuracies.add(new BigInteger(val));
			val = fw.getWho()==null?"0":fw.getWho().getAccuracy()!=null?((long)(fw.getWho().getAccuracy()*1000)+""):" ";
			accuracies.add(new BigInteger(val));
			val = fw.getDative()==null?"0":fw.getDative().getAccuracy()!=null?((long)(fw.getDative().getAccuracy()*1000)+""):" ";
			accuracies.add(new BigInteger(val));
			val = fw.getWhat()==null?"0":fw.getWhat().getAccuracy()!=null?((long)(fw.getWhat().getAccuracy()*1000)+""):" ";
			accuracies.add(new BigInteger(val));
		}
		contract.startFiveW(name, hash, bytes, claims, meta5w, accuracies).send();
	}
	
	/*
	 * METODH OF 1L, GET PAYLOAD OF RESOURCES TO BE ACKED
	 */
	public List<byte[]> getPayload(String resHash) throws Exception {
		System.out.println("START PAYLOAD...");
		List<byte[]> payload = contract.getPayload("hashhashhash").send();
		return payload;
	}
	
	/*
	 * METODH OF 1L, LEN OF NEWS SAVED
	 */
	public Integer getNewsLen() {
		try {
			return contract.newsLen().send().intValue();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
		
	}
	
	/*
	 * METODH OF 2L, LEN OF NEWS SAVED
	 */
	public Integer getNewsLen2() {
		try {
			return contractVote.newsLen().send().intValue();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return -1;
		
	}
	
	/*
	 * METODH OF 1L, GET NEWS, WHICH PAYLOAD HAS TO BE CHECKED
	 */
	public String getNewsToCheck(Integer index){
		try {
			Tuple5<String, String, byte[], BigInteger, BigInteger> ret = contract.news(new BigInteger(index+"")).send();
			if (ret.getValue5().intValue() == 1)
				return ret.getValue2();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/*
	 * METODH OF 1L, RETRIEVE NEWS AND SEND THEM TO 2L (DONE BY TRUSTEDPEER)
	 */
	public NewsBean getNewsAboveTreshold(Integer index){ //USE CASSANDRA CALL AND JUST RETRIEVE PAYLOAD
		NewsBean news = new NewsBean();//FIXME MANAGE CLAIMS
		try {
			Tuple5<String, String, byte[], BigInteger, BigInteger> ret = contract.news(new BigInteger(index+"")).send();
			if (ret.getValue5().intValue() == 2) { //CONSENSUS TRUSTINESS
				news.setName(ret.getValue1());
				news.setHash(ret.getValue2());
				news.setClaims("TODO-TODO");
				news.setTrustiness(ret.getValue4());
				return news;
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/*
	 * METODH OF 2L, NEWS TO BE VOTED
	 */
	public String getNewsToCheck2L(Integer index){
		try {
			Tuple4<String, String, BigInteger, BigInteger> ret = contractVote.news(new BigInteger(index+"")).send();
			if (ret.getValue4().intValue() == 0) //newly added
				return ret.getValue2();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/*
	 * METODH OF 2L, NEWS VOTED
	 */
	public String getNewsAcked2L(Integer index){
		try {
			Tuple4<String, String, BigInteger, BigInteger> ret = contractVote.news(new BigInteger(index+"")).send();
			if (ret.getValue4().intValue() == 1) //newly added
				return ret.getValue2();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/*
	 * METODH OF 1L, CHECK IF META-INFO 5W HAS ALREADY BEEN CHECKED
	 */
	public Boolean isHashPresent(List<it.uniroma1.dis.block.FiveW> fivew) {
		String meta5w = ""; //WHERE WHEN WHO DATIVE WHAT order
		for (it.uniroma1.dis.block.FiveW fw : fivew) {
			meta5w += fw.getWhere()==null?" ":fw.getWhere().getName()!=null?fw.getWhere().getName():" ";
			meta5w += "#+#";
			meta5w += fw.getWhen()==null?" ":fw.getWhen().getName()!=null?fw.getWhen().getName():" ";
			meta5w += "#+#";
			meta5w += fw.getWho()==null?" ":fw.getWho().getName()!=null?fw.getWho().getName():" ";
			meta5w += "#+#";
			meta5w += fw.getDative()==null?" ":fw.getDative().getName()!=null?fw.getDative().getName():" ";
			meta5w += "#+#";
			meta5w += fw.getWhat()==null?" ":fw.getWhat().getName()!=null?fw.getWhat().getName():" ";
			meta5w += "#+#";
			meta5w += "#-#"; //OR REMOVE AT THE END SENT
		}
		try {
			return contract.isHash5wPresent(meta5w).send();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return false;
		
	}
	
	/*
	 * METODH OF 1L, CHECK NEWS IN 1L (ALGORITHM CALL)
	 */
	public Boolean add5w(String hash, List<it.uniroma1.dis.block.FiveW> fivew) throws Exception {
		System.out.println("START ADD5W...");
		//list5w
		String meta5w = ""; //WHERE WHEN WHO DATIVE WHAT order
		List<BigInteger> accuracies = new ArrayList<>();
		String val;
		for (it.uniroma1.dis.block.FiveW fw : fivew) {
			meta5w += fw.getWhere()==null?" ":fw.getWhere().getName()!=null?fw.getWhere().getName():" ";
			meta5w += "#+#";
			meta5w += fw.getWhen()==null?" ":fw.getWhen().getName()!=null?fw.getWhen().getName():" ";
			meta5w += "#+#";
			meta5w += fw.getWho()==null?" ":fw.getWho().getName()!=null?fw.getWho().getName():" ";
			meta5w += "#+#";
			meta5w += fw.getDative()==null?" ":fw.getDative().getName()!=null?fw.getDative().getName():" ";
			meta5w += "#+#";
			meta5w += fw.getWhat()==null?" ":fw.getWhat().getName()!=null?fw.getWhat().getName():" ";
			meta5w += "#+#";
			meta5w += "#-#"; //OR REMOVE AT THE END SENT
			
			val = fw.getWhere()==null?"0":fw.getWhere().getAccuracy()!=null?((long)(fw.getWhere().getAccuracy()*1000)+""):"0";
			accuracies.add(new BigInteger(val));
			val = fw.getWhen()==null?"0":fw.getWhen().getAccuracy()!=null?((long)(fw.getWhen().getAccuracy()*1000)+""):"0";
			accuracies.add(new BigInteger(val));
			val = fw.getWho()==null?"0":fw.getWho().getAccuracy()!=null?((long)(fw.getWho().getAccuracy()*1000)+""):"0";
			accuracies.add(new BigInteger(val));
			val = fw.getDative()==null?"0":fw.getDative().getAccuracy()!=null?((long)(fw.getDative().getAccuracy()*1000)+""):"0";
			accuracies.add(new BigInteger(val));
			val = fw.getWhat()==null?"0":fw.getWhat().getAccuracy()!=null?((long)(fw.getWhat().getAccuracy()*1000)+""):"0";
			accuracies.add(new BigInteger(val));
		}
		TransactionReceipt t = contract.add5w(hash, meta5w, accuracies).send();
		try {
			return contract.getVoteEventEvents(t).get(0).returnValue;
		} catch(Exception e) {
			e.printStackTrace();
		}
		return false;
		
	}

	public void populateTestFiveW(Integer len) throws Exception {
		System.out.println("START POPULATE TEST5W...");
		for (int i = 0; i < len; i++)
			contract.populateTestFiveW().send();
	}
	
	/*
	 * METODH OF 2L, WHITELIST
	 */
	public void addWhitelistAddress(String address) throws Exception {
		contractVote.whitelistAddress(address).send();
	}
	
	/*
	 * METODH OF 2L, CHECKPOINT OF NEWS RETRIEVED
	 */
	public void addNewsToCheck(String name, String hash, Double trustiness, String claims) throws Exception {
		if (trustiness < 0) trustiness = 0.0; //FIX ERROR BEING UINT IN ETH
		trustiness *= 100;
		Integer trustinessValue = trustiness.intValue();
		trustinessValue *= 100; //ETHEREUM DOUBLE FORMAT
		BigInteger b = new BigInteger(trustinessValue.toString());
		contractVote.addNewsToCheck(name, hash, claims, b).send();
	}
	public void addNewsToCheck(NewsBean bean) throws Exception {
		if (bean != null && !bean.isEmpty()) {
			if (bean.getClaims() == null)
				bean.setClaims("");
			contractVote.addNewsToCheck(bean.getName(), bean.getHash(), bean.getClaims(), bean.getTrustiness()).send();
		}
	}
	
	/*
	 * METODH OF 2L, VOTE BY TRUSTEDPEERS
	 */
	public void vote(BigInteger vote, String hashResource) throws Exception {
		contractVote.checkTempNews(vote, hashResource).send();
	}
	
	/*
	 * METODH OF 1L-2L, GET NEWS IN LOOP IN 2L THEN IN 1L
	 */
	public Double getNewsTrustiness(String hashResource) throws Exception {
		//CHECK IN 2L first
		Integer len = getNewsLen();
		String hash = null;
		for (Integer i = 0; i < len; i++) {
			hash = getNewsAcked2L(i);
			if (hash.equals(hashResource))
				return 100.0; // in 2L ACK OR NACK
		}
		
		//CHECK IN 1L
		len = getNewsLen();
		NewsBean bean = null;
		for (Integer i = 0; i < len; i++) {
			bean = getNewsAboveTreshold(i);
			if (bean.getHash().equals(hashResource))
				return (Double)(bean.getTrustiness().intValue() / 100.0); // in ethereum expressed without decimal
		}
		
		//IF NOT FOUND, FAKSE BY DEFAULT
		return 0.0;
	}
	
	//get contract 1L
	public FiveW getContract() {
		return contract;
	}
}
