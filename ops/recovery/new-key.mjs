import fs from 'node:fs/promises';
import path from 'node:path';
import {fileURLToPath} from 'node:url';
import {generateKeyPairSync,createHash} from 'node:crypto';
import {privateDirectory,assert} from './recovery.mjs';
try {
 const directory=process.argv[2];assert(directory&&path.isAbsolute(directory),'Provide a NEW absolute private key directory outside the repository');
 const repo=path.resolve(path.dirname(fileURLToPath(import.meta.url)),'../..'),relative=path.relative(repo,path.resolve(directory));assert(relative.startsWith('..')||path.isAbsolute(relative),'Keys must remain outside the repository');
 await fs.mkdir(directory,{recursive:false,mode:0o700});await privateDirectory(directory);
 const keys=generateKeyPairSync('rsa',{modulusLength:3072,publicKeyEncoding:{type:'spki',format:'pem'},privateKeyEncoding:{type:'pkcs8',format:'pem'}});
 await fs.writeFile(path.join(directory,'backup-private.pem'),keys.privateKey,{flag:'wx',mode:0o600});await fs.writeFile(path.join(directory,'backup-public.pem'),keys.publicKey,{flag:'wx',mode:0o600});
 console.log(JSON.stringify({created:true,publicKeySha256:createHash('sha256').update(keys.publicKey).digest('hex'),instruction:'Move the private key to separate protected offline custody. Never commit either directory.'}));
} catch {console.error('Key creation failed. Use a new private directory; no existing key will be overwritten.');process.exitCode=1;}
