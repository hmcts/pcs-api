-- Queues the attach-document-task (AttachDocumentTaskComponent) for documents saved before DocumentEntityListener
-- existed, so CDAM attaches them to their case and stops refusing them once their TTL has passed. Spread out at
-- 10 a second so CDAM isn't asked for all of them at once.
INSERT INTO scheduled_tasks (task_name, task_instance, task_data, execution_time, picked, version)
SELECT 'attach-document-task',
       d.id::text,
       convert_to(json_build_object('caseReference', c.case_reference,
                                    'documentId', substring(d.url from '/documents/([0-9a-fA-F-]{36})'))::text,
                  'UTF8'),
       now() + row_number() OVER (ORDER BY d.submitted_date) * interval '100 milliseconds',
       false,
       1
FROM document d
JOIN pcs_case c ON c.id = d.case_id
WHERE d.type IS DISTINCT FROM 'DEFENDANT_ACCESS_CODE'
  AND d.url ~ '/documents/[0-9a-fA-F-]{36}'
ON CONFLICT DO NOTHING;
