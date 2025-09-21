# ThoughtCaddy AI Summarization Setup

This guide will help you set up the Firebase Cloud Function for AI-powered journal entry summarization using OpenAI.

## Prerequisites

1. **OpenAI API Key**: Get your API key from [OpenAI Platform](https://platform.openai.com/api-keys)
2. **Firebase CLI**: Install with `npm install -g firebase-tools`
3. **Node.js**: Version 18 or higher

## Setup Steps

### 1. Initialize Firebase Functions

```bash
# Navigate to your project directory
cd /Users/nirwan/AndroidStudioProjects/thought-caddy

# If you haven't already, initialize Firebase Functions
firebase init functions

# Choose:
# - Use existing project (thought-caddy)
# - TypeScript
# - ESLint (recommended)
# - Install dependencies
```

### 2. Install Dependencies

```bash
cd functions
npm install openai
```

### 3. Configure OpenAI API Key

You have two options for setting up the API key:

#### Option A: Using Firebase Secret Manager (Recommended for Production)

```bash
# Set the OpenAI API key as a secret
firebase functions:secrets:set OPENAI_API_KEY

# When prompted, paste your OpenAI API key
```

#### Option B: Using Environment Variables (For Local Testing)

```bash
# Create a .env file in the functions directory
echo "OPENAI_API_KEY=your_openai_api_key_here" > .env
```

### 4. Update Firebase Configuration

Make sure your `firebase.json` includes the functions configuration:

```json
{
  "functions": [
    {
      "source": "functions",
      "codebase": "default",
      "ignore": [
        "node_modules",
        ".git",
        "firebase-debug.log",
        "firebase-debug.*.log"
      ]
    }
  ]
}
```

### 5. Deploy the Function

```bash
# Deploy only the functions
firebase deploy --only functions

# Or deploy specific function
firebase deploy --only functions:summarizeEntry
```

## Testing

### Test Locally (Optional)

```bash
cd functions
npm run serve

# The function will be available at:
# http://localhost:5001/thought-caddy/us-central1/summarizeEntry
```

### Test from Android App

Once deployed, the function can be called from your Android app. The integration is already set up in:

- `AiSummaryService.kt` - Service to call the function
- `JournalRepository.kt` - Integration with journal creation
- `JournalViewModel.kt` - UI state management

## Function Details

### Endpoint
- **Name**: `summarizeEntry`
- **Type**: HTTPS Callable Function
- **Region**: us-central1
- **Authentication**: Required (Firebase Auth)

### Input
```json
{
  "text": "Your journal entry text here..."
}
```

### Output
```json
{
  "summary": "AI-generated 2-3 sentence summary of the journal entry."
}
```

### Error Handling

The function returns appropriate HTTP errors:
- `unauthenticated`: User not logged in
- `invalid-argument`: Missing or invalid text
- `resource-exhausted`: OpenAI rate limit reached
- `internal`: Server or OpenAI API errors

## Costs and Usage

### OpenAI Pricing (as of 2024)
- **GPT-4o-mini**: ~$0.15 per 1M input tokens, ~$0.60 per 1M output tokens
- **Typical journal entry**: ~100-500 tokens input, ~50-100 tokens output
- **Cost per summary**: ~$0.0001-0.0005 (very low cost)

### Firebase Functions Pricing
- **Invocations**: First 2M free, then $0.40 per 1M
- **Compute time**: First 400,000 GB-seconds free
- **Networking**: First 5GB free

## Monitoring

### View Logs
```bash
# View function logs
firebase functions:log

# View logs for specific function
firebase functions:log --only summarizeEntry
```

### Firebase Console
Monitor function usage, errors, and performance in the [Firebase Console](https://console.firebase.google.com) under Functions.

## Troubleshooting

### Common Issues

1. **"Function not found"**
   - Ensure the function is deployed: `firebase deploy --only functions`
   - Check the function name matches exactly

2. **"Missing OpenAI API key"**
   - Verify the secret is set: `firebase functions:secrets:access OPENAI_API_KEY`
   - Redeploy after setting secrets

3. **"Insufficient permissions"**
   - Ensure user is authenticated in the Android app
   - Check Firestore security rules allow authenticated access

4. **"Rate limit exceeded"**
   - OpenAI has rate limits based on your plan
   - Implement retry logic with exponential backoff

### Debug Mode

Enable debug logging by setting log level in the function:

```typescript
import { setGlobalOptions } from "firebase-functions/v2";
setGlobalOptions({ maxInstances: 10 });
```

## Security Notes

- ✅ Function requires authentication
- ✅ OpenAI API key stored as Firebase secret
- ✅ Input validation and sanitization
- ✅ Error handling without exposing internals
- ✅ Rate limiting handled gracefully

## Next Steps

1. **Test the integration** by creating journal entries in your app
2. **Monitor costs** in OpenAI and Firebase dashboards
3. **Optimize prompts** for better summaries
4. **Add retry logic** for better reliability
5. **Consider caching** for frequently accessed summaries

Your AI summarization feature is now ready! 🚀