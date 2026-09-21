# Company Master Resolver

Application governance SOP, version 1.0.0.

Input is a bounded list of existing stock_symbol IDs and source responses.
Resolve Company separately from Security and Listing.
Use jurisdiction plus registration scheme and identifier, or an exact issuer ID
within a named provider namespace. A/H records with the same provider issuer ID
may share a company; they remain different securities and listings.
Names and addresses are evidence for review, never independent merge keys.
Preserve old stock IDs, ambiguous identities and the original source response.
Conflicting registration identifiers block a merge and require review.
Output mapping IDs, identity basis, evidence IDs and unresolved records.
