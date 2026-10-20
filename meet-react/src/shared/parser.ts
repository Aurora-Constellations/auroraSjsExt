import {
  AstUtils, EmptyFileSystem, createDefaultCoreModule, createDefaultSharedCoreModule, inject,
  type AstNode, type LangiumCoreServices, type LangiumSharedCoreServices
} from 'langium';
import {
  AuroraGeneratedModule, AuroraGeneratedSharedModule
} from '@meet/grammar/module';
import type { PCM } from '@meet/grammar/ast';

const shared = inject(createDefaultSharedCoreModule(EmptyFileSystem), AuroraGeneratedSharedModule) as LangiumSharedCoreServices;
const language = inject(createDefaultCoreModule({ shared }), AuroraGeneratedModule) as LangiumCoreServices;
shared.ServiceRegistry.register(language);

type Node = AstNode & Record<string, unknown>;
const coordinateTypes = new Set(['ClinicalItem', 'IssueCoordinate', 'OrderCoordinate']);

function reject(label: string, message: string): never {
  throw new Error(`${label}: ${message}`);
}

export function parseAurora(text: string, label = 'Model'): PCM {
  const result = language.parser.LangiumParser.parse<PCM>(text);
  const lexerError = result.lexerErrors[0];
  if (lexerError) reject(label, `line ${lexerError.line ?? 1}: ${lexerError.message}`);
  const parserError = result.parserErrors[0];
  if (parserError) reject(label, `line ${parserError.token.startLine ?? 1}: ${parserError.message}`);
  const pcm = result.value;
  if (pcm.imports.length) reject(label, 'imports require module resolution and are not supported in this browser demo. Paste the resolved Clinical, Issues, and Orders sections instead.');
  if (pcm.module || pcm.template) reject(label, 'module and template declarations require expansion. Paste their resolved sections instead.');
  if (pcm.package) reject(label, 'package declarations are outside this demo’s section-based PCM representation.');

  const nodes = [pcm, ...AstUtils.streamAllContents(pcm)] as Node[];
  const coordinateNames = new Set(nodes.filter(n => coordinateTypes.has(n.$type)).map(n => String(n.name)));
  for (const node of nodes) {
    if (node.$type === 'Arith_Module' || node.$type === 'Evaluation') reject(label, 'arithmetic expressions and arith_module blocks are not evaluated by this demo.');
    if (node.$type === 'MutuallyExclusive') reject(label, 'mutually exclusive orders (or) are not represented by the current PCM algebra.');
    if (node.timestamp) reject(label, 'timestamps are not represented by the current PCM algebra.');
    if (node.snomed) reject(label, 'SNOMED annotations are not represented by the current PCM algebra.');
    if (node.$type === 'OrderCoordinate' && (node.qu as unknown[]).length) reject(label, 'individual order qualifiers are not represented by the current PCM algebra. Group qualifiers are supported.');
    if (node.$type === 'IssueCoordinate' && ((node.mods as unknown[]).length || (node.temps as unknown[]).length)) reject(label, 'from/with module references require module resolution. Paste resolved sections instead.');
    if (node.$type === 'QuReference') {
      const reference = node.ref as { $refText: string };
      if (!coordinateNames.has(reference.$refText)) reject(label, `unresolved coordinate reference “${reference.$refText}”. References must target an item in the same input model.`);
      node.ref = { $refText: reference.$refText };
    }
  }
  assertUnique(pcm.elements as unknown as Node[], label, 'section', n => n.$type);
  for (const section of pcm.elements as unknown as Node[]) {
    if (section.$type === 'Clinical' || section.$type === 'Orders') {
      const groups = section.namedGroups as Node[];
      assertUnique(groups, label, `${section.$type} group`, n => String(n.name));
      for (const group of groups) assertUnique((group.coord ?? group.orders) as Node[], label, `${String(group.name)} item`, n => String(n.name));
    } else if (section.$type === 'Issues') {
      assertUnique(section.coord as Node[], label, 'Issues item', n => String(n.name));
    }
  }
  return pcm;
}

function assertUnique(nodes: Node[], label: string, kind: string, key: (node: Node) => string): void {
  const seen = new Set<string>();
  for (const node of nodes) {
    const name = key(node);
    if (seen.has(name)) reject(label, `duplicate ${kind} “${name}”. Combine its contents in one declaration.`);
    seen.add(name);
  }
}
