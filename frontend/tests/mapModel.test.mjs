import test from 'node:test';
import assert from 'node:assert/strict';
import {mapOpportunities,featureCollection,pointBounds,safeMapUrl,validCoordinates,MAX_MAP_POINTS} from '../src/features/maps/mapModel.ts';
const item={id:1,title:'Fixture',provider:'Fixture',location:'Irecê/BA',modality:'Presencial',url:'https://example.test',latitude:-11.3,longitude:-41.85,active:true};
test('mapa só utiliza coordenadas numéricas cadastradas e anúncios elegíveis',()=>{
 const rows=[item,{...item,id:2,latitude:null},{...item,id:3,latitude:NaN},{...item,id:4,longitude:181},{...item,id:5,modality:'Remoto'},{...item,id:6,modality:'Online'},{...item,id:7,active:false},{...item,id:8,moderationStatus:'PENDING'},{...item,id:9,expiresAt:'2026-01-01'}];
 assert.deepEqual(mapOpportunities(rows,'job','2026-11-04').map(i=>i.id),[1]);assert.equal(validCoordinates('-11.3',-41.85),false);
});
test('somente cursos presenciais entram no mapa, sem gerar coordenadas a partir do município',()=>{
 const points=mapOpportunities([item,{...item,id:2,modality:'Híbrido'},{...item,id:3,modality:'Online'},{...item,id:4,latitude:null,longitude:null}],'course','2026-11-04');assert.equal(points.length,1);assert.equal(points[0].kind,'course');assert.deepEqual(pointBounds(points),[-41.85,-11.3,-41.85,-11.3]);
});
test('GeoJSON limita volume, elimina duplicados e envia somente ID e tipo ao worker',()=>{
 const points=mapOpportunities([item,item,...Array.from({length:1000},(_,i)=>({...item,id:i+2}))],'job','2026-11-04');assert.equal(points.length,MAX_MAP_POINTS);const geo=featureCollection(points);assert.equal(geo.features.length,50);assert.deepEqual(geo.features[0].properties,{id:1,kind:'job'});assert.deepEqual(geo.features[0].geometry.coordinates,[-41.85,-11.3]);assert.equal(JSON.stringify(geo).includes('provider'),false);
});
test('link de oportunidade aceita apenas HTTP/HTTPS',()=>{assert.equal(safeMapUrl('javascript:alert(1)'),null);assert.equal(safeMapUrl('data:text/html,test'),null);assert.equal(safeMapUrl('https://example.test/path'),'https://example.test/path');});
