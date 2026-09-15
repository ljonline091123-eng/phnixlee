// Neo4j 5: run constraints once before any writer starts.
CREATE CONSTRAINT company_graph_stock_code IF NOT EXISTS
FOR (c:Company) REQUIRE (c.graph_code, c.stock_code) IS UNIQUE;
CREATE CONSTRAINT industry_graph_name IF NOT EXISTS
FOR (i:Industry) REQUIRE (i.graph_code, i.name) IS UNIQUE;
CREATE CONSTRAINT event_graph_title_date IF NOT EXISTS
FOR (e:Event) REQUIRE (e.graph_code, e.title, e.date) IS UNIQUE;

// Optional skeleton seeds, each executed separately with trusted parameters.
// $master_companies comes from stock_symbol, not from LLM output.
UNWIND $master_companies AS row
MERGE (c:Company {graph_code: $graph_code, stock_code: row.stock_code})
SET c.name = row.name;

// $verified_industries comes from an approved industry taxonomy.
UNWIND $verified_industries AS row
MERGE (i:Industry {graph_code: $graph_code, name: row.name});

// New events remain in human review until supplied as $approved_events.
UNWIND $approved_events AS row
MERGE (e:Event {graph_code: $graph_code, title: row.title, date: date(row.date)});

// Dispatch validated JSON by relation type; execute ONE of the following
// parameterized statements for each batch. Never interpolate an LLM label,
// relationship type, or property name into Cypher source.
// Params: $triplets, $graph_code, $knowledge_document_id.

// BELONGS_TO: Company -> Industry
UNWIND $triplets AS row
MERGE (c:Company {graph_code: $graph_code, stock_code: row.head.key})
  ON CREATE SET c.name = row.head.name
MERGE (i:Industry {graph_code: $graph_code, name: row.tail.name})
MERGE (c)-[r:BELONGS_TO {graph_code: $graph_code}]->(i)
SET r.confidence = row.confidence,
    r.evidence_doc_ids = CASE
      WHEN $knowledge_document_id IN coalesce(r.evidence_doc_ids, [])
        THEN r.evidence_doc_ids
      ELSE coalesce(r.evidence_doc_ids, []) + $knowledge_document_id
    END;

// SUPPLIES_TO: Company -> Company
UNWIND $triplets AS row
MERGE (supplier:Company {graph_code: $graph_code, stock_code: row.head.key})
  ON CREATE SET supplier.name = row.head.name
MERGE (buyer:Company {graph_code: $graph_code, stock_code: row.tail.key})
  ON CREATE SET buyer.name = row.tail.name
MERGE (supplier)-[r:SUPPLIES_TO {graph_code: $graph_code}]->(buyer)
SET r.confidence = row.confidence,
    r.evidence_doc_ids = CASE
      WHEN $knowledge_document_id IN coalesce(r.evidence_doc_ids, [])
        THEN r.evidence_doc_ids
      ELSE coalesce(r.evidence_doc_ids, []) + $knowledge_document_id
    END;

// IMPACTED_BY: Company -> Event; event date must be ISO 8601.
UNWIND $triplets AS row
MERGE (c:Company {graph_code: $graph_code, stock_code: row.head.key})
  ON CREATE SET c.name = row.head.name
MERGE (e:Event {graph_code: $graph_code, title: row.tail.name, date: date(row.tail.date)})
MERGE (c)-[r:IMPACTED_BY {graph_code: $graph_code}]->(e)
SET r.confidence = row.confidence,
    r.evidence_doc_ids = CASE
      WHEN $knowledge_document_id IN coalesce(r.evidence_doc_ids, [])
        THEN r.evidence_doc_ids
      ELSE coalesce(r.evidence_doc_ids, []) + $knowledge_document_id
    END;
