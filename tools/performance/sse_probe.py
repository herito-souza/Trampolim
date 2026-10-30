"""Authenticated SSE capacity probe; tokens live in an ignored synthetic fixture, never output."""
import argparse, asyncio, json, os, pathlib, ssl, time, urllib.parse

async def main():
    p=argparse.ArgumentParser();p.add_argument('--fixture',required=True);p.add_argument('--base',default='http://127.0.0.1:5098');p.add_argument('--clients',type=int,default=10);p.add_argument('--seconds',type=int,default=20);p.add_argument('--output',required=True);args=p.parse_args()
    if os.environ.get('ISOLATED_TEST_ENV')!='yes':raise SystemExit('Confirm ISOLATED_TEST_ENV=yes for a synthetic environment.')
    if args.seconds<1 or args.seconds>3600:raise SystemExit('Use a duration between 1 and 3600 seconds.')
    if args.clients<1 or args.clients>5000 or args.clients>100 and os.environ.get('ALLOW_HIGH_LOAD')!='yes':raise SystemExit('High load needs suitable infrastructure and ALLOW_HIGH_LOAD=yes.')
    fixture=json.loads(pathlib.Path(args.fixture).read_text(encoding='utf-8-sig'));tokens=[a['token'] for a in fixture.get('accounts',[]) if a.get('trail')=='capacita']
    if fixture.get('synthetic') is not True or len(tokens)<args.clients or len(set(tokens))<args.clients:raise SystemExit('Provide one independently authenticated synthetic Capacita account per connection.')
    base=urllib.parse.urlparse(args.base)
    if base.scheme not in ('http','https') or base.username or base.password or base.path not in ('','/'):raise SystemExit('Invalid explicitly authorized base URL.')
    async def connect(index):
        started=time.monotonic();writer=None
        try:
            reader,writer=await asyncio.wait_for(asyncio.open_connection(base.hostname,base.port or (443 if base.scheme=='https' else 80),ssl=ssl.create_default_context() if base.scheme=='https' else None),10)
            writer.write((f'GET /api/capacita/learning/notifications/stream?after=0 HTTP/1.1\r\nHost: {base.netloc}\r\nAuthorization: Bearer {tokens[index]}\r\nAccept: text/event-stream\r\nConnection: close\r\n\r\n').encode());await writer.drain();header=await asyncio.wait_for(reader.readuntil(b'\r\n\r\n'),15)
            status=int(header.split(b' ')[1]);body=b'';deadline=time.monotonic()+args.seconds
            if status!=200:return {'status':status,'ready':False,'events':0,'seconds':round(time.monotonic()-started,2)}
            while time.monotonic()<deadline:
                try:chunk=await asyncio.wait_for(reader.read(65536),min(20,deadline-time.monotonic()))
                except asyncio.TimeoutError:break
                if not chunk:break
                body=(body+chunk)[-262144:]
            # Count event framing without retaining notification payloads in the report.
            return {'status':status,'ready':b'event:ready' in body or b'event: ready' in body,'events':body.count(b'event:'),'seconds':round(time.monotonic()-started,2)}
        except Exception:return {'status':0,'ready':False,'events':0,'seconds':round(time.monotonic()-started,2)}
        finally:
            if writer:
                writer.close()
                try:await writer.wait_closed()
                except Exception:pass
    samples=await asyncio.gather(*(connect(i) for i in range(args.clients)))
    report={'clients':args.clients,'durationRequestedSeconds':args.seconds,'connected':sum(s['status']==200 for s in samples),'ready':sum(s['ready'] for s in samples),'errors':sum(s['status']!=200 for s in samples),'samples':samples,'scope':'Connection acceptance/liveness only; does not prove delivery, revocation or multi-node recovery.'}
    pathlib.Path(args.output).write_text(json.dumps(report,indent=2),encoding='utf-8');print(json.dumps({k:v for k,v in report.items() if k!='samples'}))
    if report['errors']:raise SystemExit(1)

if __name__=='__main__':asyncio.run(main())
