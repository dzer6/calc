-- :name find-expressions :query :many
-- :doc Finds past evaluated expressions
SELECT id,
       input,
       output,
       created_at
FROM calc_history
ORDER BY created_at
LIMIT COALESCE(:limit, 25) OFFSET COALESCE(:offset, 0);

-- :name insert-expression :execute :one
-- :doc Inserts expression with calculation result
INSERT INTO calc_history (input, output)
SELECT :input, :output;