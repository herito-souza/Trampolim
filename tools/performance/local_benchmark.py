"""Safe local-only HTTP benchmark. Disposable schemas in all three development databases.
No deployment, production credentials, payments, SMTP or existing account data are used.
Requires running development Docker databases and a packaged backend JAR.
"""
import argparse, concurrent.futures, hashlib, json, os, pathlib, platform, socket, subprocess, tempfile, time, urllib.request, urllib.error, uuid

ROOT = pathlib.Path(__file__).resolve().parents[2]
DBS = [('trampolim-db','root','trampolim_db',5432,'root'),('trampolim-empresas-db','trampolim_empresas','trampolim_empresas_db',5433,'dev_empresas_only'),('trampolim-capacita-db','trampolim_capacita','trampolim_capacita_db',5434,'dev_capacita_only')]

def main():
    parser=argparse.ArgumentParser();parser.add_argument('--jar',required=True);parser.add_argument('--output',required=True);parser.add_argument('--multi-node',action='store_true');parser.add_argument('--protocol-only',action='store_true');args=parser.parse_args()
    if os.environ.get('AUDIT_ALLOW_LOCAL')!='1': raise SystemExit('Set AUDIT_ALLOW_LOCAL=1 to authorize disposable local fixtures.')
    jar=pathlib.Path(args.jar).resolve();output=pathlib.Path(args.output).resolve();schema='audit_'+uuid.uuid4().hex
    with socket.socket() as probe:
        try: probe.bind(('127.0.0.1',5098))
        except OSError: raise SystemExit('Port 5098 already occupied; no process was changed.')
    with socket.socket() as probe:
        try: probe.bind(('127.0.0.1',5099))
        except OSError: raise SystemExit('Private management port 5099 occupied; no process was changed.')
    def sql(db, statement):
        result=subprocess.run(['docker','exec','-i',db[0],'psql','-X','-v','ON_ERROR_STOP=1','-U',db[1],'-d',db[2],'-At'],input=statement,text=True,capture_output=True)
        if result.returncode: raise RuntimeError('Local fixture SQL failed: '+result.stderr[-500:])
        return result.stdout.strip()
    def request(path, token=None, payload=None):
        headers={'Content-Type':'application/json'}
        if token: headers['Authorization']='Bearer '+token
        req=urllib.request.Request('http://127.0.0.1:5098/api'+path,data=None if payload is None else json.dumps(payload).encode(),headers=headers)
        started=time.perf_counter()
        try:
            with urllib.request.urlopen(req,timeout=10) as response: return response.status,response.read(),(time.perf_counter()-started)*1000
        except urllib.error.HTTPError as error: return error.code,error.read(),(time.perf_counter()-started)*1000
        except Exception: return 0,b'',(time.perf_counter()-started)*1000
    process=None;second=None;created=[];temp=tempfile.TemporaryDirectory(prefix='trampolim-audit-');log=None
    try:
        for db in DBS: sql(db,'CREATE SCHEMA '+schema);created.append(db)
        env=os.environ.copy();env.update({'SERVER_ADDRESS':'127.0.0.1','SERVER_PORT':'5098','MANAGEMENT_PORT':'5099','MANAGEMENT_ADDRESS':'127.0.0.1','MAIL_ENABLED':'false','CATALOG_JOBS_ENABLED':'false','CAPACITA_MEDIA_ROOT':str(pathlib.Path(temp.name)/'private-media'),'CAPACITA_ADMIN_EMAILS':'audit@example.test','JWT_SECRET':'audit-only-opportunities-signing-secret-0123456789','EMPRESAS_JWT_SECRET':'audit-only-enterprises-signing-secret-0123456789','CAPACITA_JWT_SECRET':'audit-only-education-signing-secret-0123456789','MERCADOPAGO_ACCESS_TOKEN':'','OPENAI_API_KEY':'','MONITORING_KEY':'audit-local-monitoring-key-0123456789-abcdef'})
        for prefix,db in zip(['OPORTUNIDADES','EMPRESAS','CAPACITA'],DBS):
            env[prefix+'_DB_URL']=f'jdbc:postgresql://127.0.0.1:{db[3]}/{db[2]}?currentSchema={schema}'
            env[prefix+'_DB_USERNAME']=db[1];env[prefix+'_DB_PASSWORD']=db[4]
        log=open(pathlib.Path(temp.name)/'server.log','w',encoding='utf-8')
        process=subprocess.Popen(['java','-Xms128m','-Xmx768m','-jar',str(jar)],cwd=ROOT/'backend',env=env,stdout=log,stderr=subprocess.STDOUT,creationflags=getattr(subprocess,'CREATE_NO_WINDOW',0))
        for _ in range(120):
            if process.poll() is not None: raise RuntimeError('Temporary backend did not start; consult local log '+str(pathlib.Path(temp.name)/'server.log'))
            if request('/health')[0]==200: break
            time.sleep(.5)
        else: raise RuntimeError('Temporary backend readiness timed out')
        password='Audit-Synthetic-Only-42!';email='audit@example.test'
        student={'name':'Synthetic audit student','email':email,'password':password,'educationStage':'HIGH_SCHOOL','ageBand':'AGE_18_PLUS','termsAccepted':True}
        status,body,_=request('/capacita/auth/register',payload=student)
        if status not in (200,201): raise RuntimeError('Synthetic registration failed '+str(status))
        student_token=json.loads(body)['token']
        person={'name':'Synthetic audit candidate','email':email,'password':password,'termsAccepted':True}
        status,body,_=request('/auth/cadastro',payload=person)
        if status not in (200,201): raise RuntimeError('Synthetic candidate registration failed '+str(status))
        person_token=json.loads(body)['token']
        corporate={'name':'Synthetic audit employer','email':email,'password':password,'legalName':'Synthetic audit company','tradeName':'Synthetic audit company','cnpj':'11222333000181','phone':'11999990000','website':'','industry':'Test','address':'Synthetic address','city':'Test','state':'SP','termsAccepted':True}
        status,body,_=request('/empresas/auth/register',payload=corporate)
        if status not in (200,201): raise RuntimeError('Synthetic corporate registration failed '+str(status))
        company_token=json.loads(body)['token']
        sql(DBS[2],f"SET search_path TO {schema}; INSERT INTO learning_courses(teacher_id,title,category,status) SELECT id,'Synthetic audit course','Test','PUBLISHED' FROM student_users WHERE email='audit@example.test'; INSERT INTO learning_subjects(course_id,title,sort_order) SELECT c.id,'Subject '||g,g FROM learning_courses c CROSS JOIN generate_series(1,20) g; INSERT INTO learning_modules(subject_id,title,sort_order) SELECT s.id,'Module '||g,g FROM learning_subjects s CROSS JOIN generate_series(1,10) g; INSERT INTO learning_lessons(module_id,title,status,media_key,duration_seconds,validation_status) SELECT m.id,'Lesson '||g,'PUBLISHED',gen_random_uuid(),180,'READY' FROM learning_modules m CROSS JOIN generate_series(1,5) g;")
        course_id=sql(DBS[2],f'SET search_path TO {schema}; SELECT min(id) FROM learning_courses;').splitlines()[-1]
        routes=[('/capacita/learning/courses?limit=25',student_token),('/capacita/learning/courses/'+course_id,student_token),('/capacita/learning/notifications',student_token),('/capacita/tracks',None),('/public/companies/internships?limit=25',None),('/profile',person_token),('/opportunities/job',person_token),('/company/dashboard',company_token)]
        for path,token in routes:
            code,_,_=request(path,token)
            if code!=200: raise RuntimeError('Representative route unavailable '+path+' status='+str(code))
        monitoring={'available':False}
        try:
            def management(path,key=None):
                req=urllib.request.Request('http://127.0.0.1:5099'+path,headers={} if key is None else {'X-Monitoring-Key':key})
                try:
                    with urllib.request.urlopen(req,timeout=5) as result:return result.status,result.read()
                except urllib.error.HTTPError as error:return error.code,error.read()
            denied,_=management('/actuator/prometheus');allowed,metrics=management('/actuator/prometheus','audit-local-monitoring-key-0123456789-abcdef');alive,_=management('/actuator/health/liveness')
            monitoring={'available':True,'unauthenticatedStatus':denied,'authorizedStatus':allowed,'livenessStatus':alive,'sseGaugePresent':b'capacita_sse_connections' in metrics,'poolGaugePresent':b'hikaricp_connections_max' in metrics}
            if denied!=403 or allowed!=200 or alive!=200:raise RuntimeError('Private management authorization failed')
        except urllib.error.URLError:pass  # Baseline predates Actuator.
        login_results=[]
        for path in ['/auth/login','/empresas/auth/login','/capacita/auth/login']:
            code,_,ms=request(path,payload={'email':email,'password':password});login_results.append({'route':path,'status':code,'milliseconds':round(ms,2)})
        isolation=[]
        for path,token in [('/company/dashboard',student_token),('/capacita/learning/me',company_token),('/profile',student_token)]:
            code,_,_=request(path,token);isolation.append({'route':path,'status':code,'rejected':code in (401,403)})
        results=[]
        # At most 100 local clients, short progressive stages. Not a 5,000-user certification.
        for concurrency,seconds,name in ([] if args.protocol_only else [(10,4,'warmup'),(25,8,'nominal'),(50,5,'stress'),(100,2,'short-burst'),(10,12,'short-resistance')]):
            deadline=time.monotonic()+seconds
            def worker(index):
                values=[];iteration=0
                while time.monotonic()<deadline:
                    path,token=routes[(index+iteration)%len(routes)];code,body,ms=request(path,token);values.append((code,ms,len(body)));iteration+=1;time.sleep(.05)
                return values
            started=time.monotonic()
            with concurrent.futures.ThreadPoolExecutor(max_workers=concurrency) as pool: samples=[sample for rows in pool.map(worker,range(concurrency)) for sample in rows]
            elapsed=time.monotonic()-started;lat=sorted(row[1] for row in samples);errors=sum(row[0]==0 or row[0]>=500 for row in samples);unexpected=sum(row[0]!=200 for row in samples)
            percentile=lambda p:round(lat[min(len(lat)-1,int((len(lat)-1)*p))],2)
            results.append({'stage':name,'clients':concurrency,'seconds':round(elapsed,2),'requests':len(samples),'rps':round(len(samples)/elapsed,2),'p50_ms':percentile(.5),'p95_ms':percentile(.95),'p99_ms':percentile(.99),'server_or_transport_errors':errors,'unexpected_statuses':unexpected,'error_rate':round(errors/len(samples),6),'responses_bytes':sum(row[2] for row in samples)})
        multi_node={'executed':False}
        if args.multi_node:
            with socket.socket() as probe:
                try:probe.bind(('127.0.0.1',5096))
                except OSError:raise RuntimeError('Secondary test port occupied')
            env2=env.copy();env2.update({'SERVER_PORT':'5096','MANAGEMENT_PORT':'0'})
            second=subprocess.Popen(['java','-Xms128m','-Xmx768m','-jar',str(jar)],cwd=ROOT/'backend',env=env2,stdout=log,stderr=subprocess.STDOUT,creationflags=getattr(subprocess,'CREATE_NO_WINDOW',0))
            for _ in range(120):
                if second.poll() is not None:raise RuntimeError('Secondary node did not start')
                try:
                    with urllib.request.urlopen('http://127.0.0.1:5096/api/health',timeout=2) as response:
                        if response.status==200:break
                except Exception:pass
                time.sleep(.5)
            else:raise RuntimeError('Secondary node readiness timed out')
            other=student.copy();other['email']='audit-other@example.test';other['name']='Synthetic independent student'
            code,body,_=request('/capacita/auth/register',payload=other)
            if code!=200:raise RuntimeError('Second synthetic account failed')
            other_token=json.loads(body)['token']
            def stream(port,cursor):
                req=urllib.request.Request(f'http://127.0.0.1:{port}/api/capacita/learning/notifications/stream?after={cursor}',headers={'Authorization':'Bearer '+student_token,'Accept':'text/event-stream'})
                return urllib.request.urlopen(req,timeout=12)
            def await_question(connection,question):
                cursor=0
                for _ in range(100):
                    line=connection.readline().decode('utf-8').strip()
                    if line.startswith('id:'):cursor=int(line[3:].strip())
                    if line.startswith('data:'):
                        data=json.loads(line[5:].strip())
                        if any(item.get('question_id')==question for item in data.get('items',[])):return cursor
                raise RuntimeError('Persisted cross-node notification missing')
            def create_question():
                code,body,_=request('/capacita/learning/discussion/questions',other_token,{'clientKey':str(uuid.uuid4()),'courseId':int(course_id),'title':'Synthetic cross-node question','body':'Synthetic text only'})
                if code!=200:raise RuntimeError('Cross-node synthetic question failed '+str(code))
                return json.loads(body)['id']
            with stream(5096,0) as connection:
                first=create_question();cursor=await_question(connection,first)
            second_question=create_question()
            with stream(5098,cursor) as connection:recovered=await_question(connection,second_question)
            code,_,_=request('/capacita/session/logout',student_token,{})
            if code!=204:raise RuntimeError('Synthetic logout failed')
            req=urllib.request.Request('http://127.0.0.1:5096/api/capacita/learning/notifications',headers={'Authorization':'Bearer '+student_token})
            try:
                with urllib.request.urlopen(req,timeout=5) as response:revoked=response.status
            except urllib.error.HTTPError as error:revoked=error.code
            if revoked not in (401,403):raise RuntimeError('Revoked token accepted on second node')
            if request('/company/dashboard',company_token)[0]!=200:raise RuntimeError('Capacita logout affected Empresas')
            multi_node={'executed':True,'nodes':2,'deliveryAcrossNodes':cursor>0,'offlineRecoveryOnOtherNode':recovered>cursor,'crossNodeRevocation':revoked in (401,403),'independentCorporateSessionPreserved':True}
        report={'schemaVersion':1,'multiNode':multi_node,'jarBytes':jar.stat().st_size,'jarSHA256':hashlib.sha256(jar.read_bytes()).hexdigest(),'monitoring':monitoring,'environment':{'os':platform.platform(),'logicalCPUs':os.cpu_count(),'heapMaximumMiB':768,'backendInstances':1,'database':'three local development PostgreSQL containers, disposable schemas','accounts':3,'course':{'subjects':20,'modules':200,'lessons':1000},'mediaDistributionIncluded':False,'clientsShareSyntheticAccounts':True},'loginSamples':login_results,'crossTrailIsolation':isolation,'stages':results}
        output.parent.mkdir(parents=True,exist_ok=True);output.write_text(json.dumps(report,indent=2),encoding='utf-8');print(json.dumps(report,indent=2))
    finally:
        if second:
            second.terminate()
            try:second.wait(timeout=35)
            except subprocess.TimeoutExpired:second.kill();second.wait()
        if process:
            process.terminate()
            try: process.wait(timeout=35)
            except subprocess.TimeoutExpired: process.kill();process.wait()
        if log: log.close()
        for db in reversed(created): sql(db,'DROP SCHEMA '+schema+' CASCADE')
        temp_path=pathlib.Path(temp.name).resolve();temp_root=pathlib.Path(tempfile.gettempdir()).resolve()
        if temp_path.parent==temp_root and temp_path.name.startswith('trampolim-audit-'): temp.cleanup()

if __name__=='__main__': main()
