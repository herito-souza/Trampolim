import {test} from 'node:test';
import assert from 'node:assert/strict';
import {formatCnpj,formatPhone} from '../src/components/forms/formRules.ts';
test('CNPJ preserva dígitos com máscara sem alegar verificação cadastral',()=>{assert.equal(formatCnpj('12345678000190'),'12.345.678/0001-90');assert.equal(formatCnpj('12.345.678/0001-90'),'12.345.678/0001-90');assert.equal(formatCnpj('AB123'),'AB123');});
test('telefone permite formato brasileiro e preserva números internacionais',()=>{assert.equal(formatPhone('74999998888'),'(74) 99999-8888');assert.equal(formatPhone('+55 74 99999-8888'),'+55 74 99999-8888');assert.equal(formatPhone('123456789012'),'123456789012');});
