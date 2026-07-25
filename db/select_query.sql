SELECT queryID, title, description, resolvedStatus, queryDate
FROM Query
WHERE userID = <your userID>
ORDER BY queryDate DESC;