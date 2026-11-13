import test from 'node:test';
import assert from 'node:assert/strict';
import { matchesSearch, normalizeSearchText } from '../src/components/workspace/universalSearchModel.ts';

test('busca contextual ignora maiúsculas e acentos sem alterar o termo exibido',()=>{
  assert.equal(normalizeSearchText('  Candidaturas  '),'candidaturas');
  assert.equal(matchesSearch('diagnostico','Diagnóstico vocacional'),true);
  assert.equal(matchesSearch('IRECÊ','Vagas em Irece'),true);
  assert.equal(matchesSearch('  ','Vagas'),false);
  assert.equal(matchesSearch('vaga','Cursos de apoio'),false);
});
