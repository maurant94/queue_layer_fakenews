pragma solidity ^0.4.18;

import "./string.sol";

contract FiveW {

using strings for *;

uint DECIMAL = 10000;

enum State { NewlyCreated, ConsensusFiveW, ConsensusTrustiness}

string[] public test; //just for DEBUG

struct FiveWSentence {
string whoName;
uint whoAccuracy;
string dativeName;
uint dativeAccuracy;
string whatName;
uint whatAccuracy;
string whereName;
uint whereAccuracy;
string whenName;
uint whenAccuracy;
//for now why not used
//string whyName;
//uint whyAccuracy;
}

FiveWSentence[]  questions; //all just for TEST
// future work, prepare predefined list of 5w where to add according to trustiness, in order to speed up, but consuming memory

struct Metainfo {
string name;
string  hash;
string hash5w;
mapping (bytes32 => FiveWSentence[]) FiveWMap;
string[] claim;
uint trustiness; //suppose trustiness in % 100,00 so 10^3

State state;
}

mapping (uint => Metainfo) public news;
uint newsLen = 0;
mapping (string => uint) newsID; //the key hash of metainfo is used for update purpose

struct Vote {
address[] adrs;
FiveWSentence[] exctracted5w;
bytes32 shaOf5w;
}
mapping (string => string[]) votesRes; //key is resource hash, the other is ID of vote struct
mapping (string => Vote) votes;
//new id is hashRes+hash5w {voteRes containes if for votes
mapping (string => byte[]) payloads; //struct for saving resource whose 5w consensus has to be achieved

/*
NOW STARTS THE FIRST CALL, WE JUST SAVE METAINFO AS NEWLY CREATED AND PAYLOAD
*/
function startFiveW(string name,
string  hash,
byte[] payloadRes, //USED AS INPUT FOR EXTRACTING FIVEW
string claims //WITH "-" AS DELIMITER FOR STRINGS
//string meta5w //SEPARATED BY CUSTOM DELIMITER, FOR NOW SKIP TILL IMPLEMENTED
) public {

Metainfo memory meta;
meta.name = name;
meta.hash = hash; //MAYBE COMPUTE
meta.claim = split(claims,"-");
meta.state = State.NewlyCreated;

newsID[hash] = newsLen + 1; //increment counter
newsLen++;
news[newsID[hash]] = meta; //PUT

payloads[hash] = payloadRes;

}
//FOR TEST:
/*
before populate with start5w : "prova", "reshash",["0x2345"],"claim1-claim2"
now invoke add5w:
"reshash","a#+#b#+#a#+#a#+#a#+##-#a#+#a#+#a#+#a#+#a#+#",[1,1,1,1,1,1,1,1,1,1]
*/
function add5w(string resHash, string extracted, uint[] extAccuracy) public {
bool found = false;
string[] memory parts = split5w(extracted, extAccuracy);
FiveWSentence[] memory list = new FiveWSentence[](5); //for now declerad fixed maximum size 5
FiveWSentence memory fivew;
uint i; //for loops
for(i = 0; i < extAccuracy.length/5; i++){
if (i > 5) break; //MAX SIZE BY DEFAULT
fivew.whereName = parts[5*i];
fivew.whenName = parts[5*i+1];
fivew.whoName = parts[5*i+2];
fivew.dativeName = parts[5*i+3];
fivew.whatName = parts[5*i+4];
fivew.whereAccuracy = extAccuracy[5*i];
fivew.whenAccuracy = extAccuracy[5*i+1];
fivew.whoAccuracy = extAccuracy[5*i+2];
fivew.dativeAccuracy = extAccuracy[5*i+3];
fivew.whatAccuracy = extAccuracy[5*i+4];
list[i] = fivew;
}
string memory hash; //used later for update


//after read 5w list check and add
if (votesRes[resHash].length != 0){
for (i = 0; i < votesRes[resHash].length; i++) {
if (sha256(abi.encodePacked(list)) == (votes[votesRes[resHash][i]].shaOf5w)) { //check if same 5w already extracted
votes[votesRes[resHash][i]].adrs.push(msg.sender);
hash = votesRes[resHash][i];
found = true;
break;
}
}
}

if (!found && extAccuracy.length > 5) {
Vote storage v;
v.adrs.push(msg.sender);
for (i = 0; i < extAccuracy.length/5; i++) {
v.exctracted5w.push(list[i]);
}
votesRes[resHash].push(resHash.toSlice().concat("hash5w".toSlice()));
//HERE ERROR
votes[resHash.toSlice().concat("hash5w".toSlice())] = v;
}

if (found && votesRes[resHash].length > 10){ //DEFINE A TRESHOLD AND TRIGGER TRUSTINESS AND SO ON
//consensus achieved

//compute trustiness
//ALGORITHM
uint trustValue = 1;
//for all string (5w) extracted computeTrustiness, then average, but now do algorithm just one time to test
//for testing just one
for (i = 0; i < list.length; i++) {
trustValue += computeTrustiness(list[i].whoName,list[i].whereName,list[i].whenName, list[i].whatName, list[i].dativeName);
//at the end compute evarage by dividing for lenght
}
trustValue /= i;
//Metainfo meta = news[newsID[hash]]; CURRENT VARIABLE TO BE UPDATED
news[newsID[hash]].state = State.ConsensusFiveW;
news[newsID[hash]].trustiness = computeTrustiness("Bob","Rome","April", "met", "Alice");
if (trustValue > 2000) { //EXAMPLE 20%
news[newsID[hash]].state = State.ConsensusTrustiness;
//VARIABLE UPDATED OK
} else {
delete news[newsID[hash]];
}
//consensus whole meta NO NEED
//now let's clear
delete payloads[hash];
delete votesRes[hash];
delete votes[hash];

//now everything is complete, we want just to add the resource do cassandra FIXME
}
}

/*
* FUNCTION ALGORITHM TRUSTINESS
*
*
*/
function computeTrustiness(string who, string where, string when, string what, string dative) internal view returns(uint) { //other
uint T = 8000; //treshold for trustiness uint N = T/10; //max num of document to be analyzed
uint critical = 2000; //critical value (minimum for trusted news)
uint accuracy = 8000; //defined as trustiness of document

while (T > critical) {

while (accuracy > 1) {
//compute P(A|Where&When)*P(B|Where&When)*P(Action|Where&When)*P(Action|A)*P(Action|B)
//LET US DEFINE AN ARRAY OF UINT OTHERWISE STACK TOO DEEP (MAX 16)
//error DIVISION BY ZERO
uint[] memory probCount = new uint[](8);
probCount[0] = 0; //countContext
probCount[1] = 0; //countActorinContext
probCount[2] = 0; //countDativeinContext
probCount[3] = 0; //countActioninContext
probCount[4] = 0; //countActor
probCount[5] = 0; //countDative
probCount[6] = 0; //countActioninActor
probCount[7] = 0; //countActioninDative
for (uint i = 0; i < questions.length; i++){
if (compareStrings(questions[i].whereName,where) &&
compareStrings(questions[i].whenName,when) &&
questions[i].whereAccuracy >= accuracy &&
questions[i].whenAccuracy >= accuracy) {
probCount[0] += 1;
if (compareStrings(questions[i].whoName,who)) {
probCount[1] = probCount[1] + questions[i].whoAccuracy;
}
if (compareStrings(questions[i].dativeName,dative)) {
probCount[2] = probCount[2] + questions[i].dativeAccuracy;
}
if (compareStrings(questions[i].whatName,what)) {
probCount[3] = probCount[3] + questions[i].whatAccuracy;
}
}
if (compareStrings(questions[i].whoName,who) &&
questions[i].whoAccuracy >= accuracy) {
probCount[4] += 1;
if (compareStrings(questions[i].whatName,what)) {
probCount[6] = probCount[6] + questions[i].whatAccuracy;
}
}
if (compareStrings(questions[i].dativeName,dative) &&
questions[i].dativeAccuracy >= accuracy) {
probCount[5] += 1;
if (compareStrings(questions[i].whatName,what)) {
probCount[7] = probCount[7] + questions[i].whatAccuracy;
}
}
}
if (probCount[0] + probCount[4] + probCount[5] > T/100) { //FIXME TRESHOLD VALUE INSTEAD OF 0 (N)
//you can end and calculate value (THE FIRST PART INVOLVES ACCURACY)
//divide by decimal^num if multiplication
if(probCount[0] == 0) return uint(100);
if(probCount[4] == 0) return uint(104);
if(probCount[5] == 0) return uint(105);
//MAYBE MANAGE SWITCH CASE INSTEAD OF RETURNING ERROR
return (accuracy)*(probCount[1]/probCount[0])*(probCount[2]/probCount[0])*
(probCount[3]/probCount[0])*(probCount[6]/probCount[4])
*(probCount[7]/probCount[5])/(DECIMAL**5);
}
accuracy -= 1000;
}
T -= 1000;
}
return uint(0);
}

function getPayload(string hash) public view returns (byte[]) {
return payloads[hash];
}

function populateTestFiveW() public {
FiveWSentence memory fivew;
fivew.whereName = "Rome";
fivew.whenName = "April";
fivew.whoName = "Bob";
fivew.dativeName = "Tom";
fivew.whatName = "met";
fivew.whenAccuracy = 8000;
fivew.whereAccuracy = 8000;
fivew.whoAccuracy = 8000;
fivew.dativeAccuracy = 8000;
fivew.whatAccuracy = 8000;
questions.push(fivew);
questions.push(fivew);
questions.push(fivew);
}

function compareStrings (string a, string b) internal pure returns (bool){
return keccak256(abi.encodePacked(a)) == keccak256(abi.encodePacked(b));
}

function split(string claims, string separator) internal pure returns (string[]) {
var s = claims.toSlice();
var delim = separator.toSlice();
var parts = new string[](s.count(delim));
for(uint i = 0; i < parts.length; i++) {
parts[i] = s.split(delim).toString();
}
return parts;
}

function split5wUtil(string sentences) internal pure returns (string[]) {
//multiple sentence delimier #+#, for each part (non empty) #-#
//example TEST "a#+#b#+#a#+#a#+#a#+##-#a#+#a#+#a#+#a#+#a#+#",[1,1,1,1,1,1,1,1,1,1]
string[] memory s = split(sentences,"#-#");
string[] memory ret = new string[](10);
for (uint i = 0; i < s.length; i++){
string[] memory tmp = split(s[i],"#+#");
for (uint j = 0; j <5; j++) {
ret[5*i+j] = tmp[j];
}
}
return ret;
}

function split5w(string sentences, uint[] accuracies) internal returns (string[]) {
FiveWSentence[] storage fw;
FiveWSentence memory fivew;
test = split5wUtil(sentences);
string[] parts = test;

return parts;
}
}
