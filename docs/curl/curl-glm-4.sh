curl -X POST \
        -H "Authorization: Bearer eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiIsInNpZ25fdHlwZSI6IlNJR04ifQ.eyJhcGlfa2V5IjoiNTA4MzhmZGQ2ZWUzNDE0NTk0ZTQ3ZWNlMGNkNWJlMzAiLCJleHAiOjE3Nzc3ODg4NjI4MjEsInRpbWVzdGFtcCI6MTc3Nzc4NzA2Mjg0MX0.uxYfkvh7P9O0_191KcStzHcXionPl--xrD37EGLWABQ
" \
        -H "Content-Type: application/json" \
        -H "User-Agent: Mozilla/4.0 (compatible; MSIE 5.0; Windows NT; DigExt)" \
        -d '{
          "model":"glm-4",
          "stream": "true",
          "messages": [
              {
                  "role": "user",
                  "content": "1+1"
              }
          ]
        }' \
  https://open.bigmodel.cn/api/paas/v4/chat/completions
